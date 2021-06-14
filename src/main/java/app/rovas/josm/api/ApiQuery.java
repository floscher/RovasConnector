package app.rovas.josm.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

import app.rovas.josm.gen.PluginVersion;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.TeeInputStream;
import app.rovas.josm.util.UrlProvider;

/**
 * A query to an API endpoint.
 * The preferred way to query is {@link #query(ApiCredentials, Consumer, Consumer)} with callbacks.
 * But there's also {@link #query(ApiCredentials)} that will return for success and throw an
 * {@link ApiException} in case of an error.
 * @param <EC> the type of error code that will be returned in case of an error
 */
public abstract class ApiQuery<EC extends ApiQuery.ErrorCode> {
  private static final Pattern POSITIVE_INT_PATTERN = Pattern.compile("^-?[0-9]+$");

  protected final UrlProvider urlProvider;
  protected final URL queryUrl;

  /**
   * Creates a new API query
   * @param urlProvider the URL provider from which we can obtain URLs
   * @param queryUrl our endpoint URL that we will query
   */
  public ApiQuery(final UrlProvider urlProvider, final URL queryUrl) {
    this.urlProvider = urlProvider;
    this.queryUrl = queryUrl;
  }


  /**
   * @return an array of all "known" error codes that the server returns
   */
  protected abstract EC[] getKnownErrorCodes();

  /**
   * Creates a custom error code (used for errors that are not returned by the API, like connection errors or similar)
   * @param code the error code, can be empty if the API didn't return a number
   * @param translatableMessage a message describing the error, this should be a message that can be passed to {@link I18n#tr(String, Object...)}
   * @return the newly created error code
   */
  protected abstract EC createAdditionalErrorCode(final Optional<Integer> code, final String translatableMessage);

  /**
   * @param credentials the user's API credentials
   * @return the response returned by the server, usually responses > 0 are successes, others are errors, but depends on the specific query
   * @throws ApiException.ConnectionFailure if the response can't even be read
   * @throws ApiException.DecodeResponse if the response can be read but not decoded as JSON
   * @throws ApiException.WrongPluginApiCredentials if the API credentials are invalid
   * @throws ApiException see the more specific descriptions of the subclasses
   */
  protected abstract int query(final ApiCredentials credentials) throws ApiException;

  /**
   * A wrapper for {@link #query(ApiCredentials)} that uses callbacks instead of throwing exceptions
   * @param credentials the user's credentials that are used for this request
   * @param successCallback in case a successful result was retrieved, the result will be passed to this consumer as a positive integer
   * @param errorCallback in case an error occurs, the error code will be passed to this consumer
   */
  @SuppressWarnings("PMD.AvoidInstanceofChecksInCatchClause")
  public void query(
    final ApiCredentials credentials,
    final Consumer<Integer> successCallback,
    final Consumer<EC> errorCallback
  ) {
    try {
      final int result = query(credentials);
      final Optional<EC> errorCode = Stream.of(getKnownErrorCodes())
        .filter(it -> it.getCode().map(code -> code == result).orElse(false))
        .findFirst();
      if (errorCode.isPresent()) {
        errorCallback.accept(errorCode.get());
      } else {
        final Optional<Integer> success = Optional.of(result).filter(it -> it > 0);
        if (success.isPresent()) {
          Logging.debug("[rovas] API query successful ({0})", success.get());
          successCallback.accept(success.get());
        } else {
          errorCallback.accept(createAdditionalErrorCode(Optional.of(result), I18n.marktr("An unknown error occurred!")));
        }
      }
    } catch (final ApiException e) {
      errorCallback.accept(createAdditionalErrorCode(Optional.empty(), e.getMessage()));
      if (e.isShouldBeReportedAsBug()) {
        BugReportQueue.getInstance().submit(new ReportedException(e));
      }
    }
  }

  /**
   * Sends a POST request to {@link #queryUrl}, authorizing with the given credentials and sending the given JSON request
   * @param credentials the credentials that should be presented to the API server in order to authorize the request
   * @param requestContent the JSON request that should be sent
   * @return the opened {@link URLConnection} after the request has been sent already
   * @throws ApiException.ConnectionFailure if in the process a connection error occured
   */
  protected URLConnection sendPostRequest(final ApiCredentials credentials, final JsonObjectBuilder requestContent) throws ApiException.ConnectionFailure {
    final URLConnection connection;
    try {
      connection = queryUrl.openConnection();
    } catch (final IOException e) {
      throw new ApiException.ConnectionFailure(queryUrl, e);
    }
    try {
      connection.setRequestProperty("API-KEY", credentials.getApiKey());
      connection.setRequestProperty("TOKEN", credentials.getApiToken());
      connection.setRequestProperty("Content-Type", "application/json;charset=" + StandardCharsets.UTF_8.name());
      connection.setRequestProperty("User-Agent", "JOSM-rovas/" + PluginVersion.VERSION_NAME);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Accept-Charset", StandardCharsets.UTF_8.name());
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setConnectTimeout(10_000);
      connection.setReadTimeout(10_000);
      if (connection instanceof HttpURLConnection) {
        ((HttpURLConnection) connection).setChunkedStreamingMode(0);
        ((HttpURLConnection) connection).setRequestMethod("POST");
      }
      try (Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
        final String request = requestContent.build().toString();
        Logging.debug("[rovas] API request:\n{0}", request);
        writer.write(request);
      }
    } catch (IOException e) {
      disconnect(connection);
      throw new ApiException.ConnectionFailure(queryUrl, e);
    }
    return connection;
  }

  /**
   * Decodes JSON in the form {@code {"result": "42"}} from the given input stream.
   * @param connection the connection from which the response is read
   * @param key the JSON key to which the resulting value is mapped
   */
  protected static int decodeJsonResult(final URLConnection connection, final String key) throws ApiException {
    final ByteArrayOutputStream capture = new ByteArrayOutputStream();
    try (ByteArrayOutputStream capture2 = capture) {
      if (connection instanceof HttpURLConnection && HttpURLConnection.HTTP_UNAUTHORIZED == ((HttpURLConnection) connection).getResponseCode()) {
        throw new ApiException.WrongPluginApiCredentials(connection.getURL());
      }
      try (TeeInputStream stream = new TeeInputStream(connection.getInputStream(), capture2)) {
        return Optional.ofNullable(Json.createReader(stream).readObject())
          .flatMap(it ->
            Optional.ofNullable(it.get(key))
              .flatMap(result ->
                result instanceof JsonString
                  ? Optional.ofNullable(((JsonString) result).getString())
                  .filter(string -> POSITIVE_INT_PATTERN.matcher(string).matches())
                  .map(Integer::parseInt)
                  : (
                  result instanceof JsonNumber
                    ? Optional.of(((JsonNumber) result).intValue())
                    : Optional.empty()
                )
              )
          ).orElseThrow(() -> {
            Logging.warn(MessageFormat.format("Can''t decode this ({0} bytes):\n{1}", capture.toByteArray().length, new String(capture.toByteArray(), StandardCharsets.UTF_8)));
            return new ApiException.DecodeResponse(connection.getURL(), null);
          });
      }
    } catch (JsonException je) { // can be thrown by readObject()
      Logging.warn("Can''t decode this ({0} bytes):\n{1}", capture.toByteArray().length, new String(capture.toByteArray(), StandardCharsets.UTF_8));
      throw new ApiException.DecodeResponse(connection.getURL(), je);
    } catch (IOException e) {
      throw new ApiException.ConnectionFailure(connection.getURL(), e);
    } finally {
      disconnect(connection);
    }
  }

  private static void disconnect(final URLConnection connection) {
    Utils.instanceOfAndCast(connection, HttpURLConnection.class).ifPresent(HttpURLConnection::disconnect);
  }

  /**
   * An error state for an API request.
   */
  public static class ErrorCode {
    /**
     * Server errors will have an error code, other errors (like connection issues) will have an empty code.
     */
    @NotNull
    private final Optional<Integer> code;
    /**
     * A message that can be passed to {@link I18n#tr(String, Object...)} and shown to the user
     */
    @NotNull
    private final String translatableMessage;

    /** Creates a new error code */
    public ErrorCode(@NotNull final Optional<Integer> code, @NotNull final String translatableMessage) {
      this.code = Objects.requireNonNull(code);
      this.translatableMessage = Objects.requireNonNull(translatableMessage);
    }

    /**
     * @return the error code, might be empty if an unknown error occured that doesn't have a number yet
     */
    @NotNull
    public Optional<Integer> getCode() {
      return code;
    }

    /**
     * @return the error message, should preferably be a string that can be translated by {@link I18n#tr(String, Object...)}
     */
    @NotNull
    public String getTranslatableMessage() {
      return translatableMessage;
    }

    @Override
    public String toString() {
      return "ErrorCode " +
        code.map(String::valueOf).orElse("‹null›") +
        " (message: \"" + I18n.tr(translatableMessage) + "\")";
    }
  }
}
