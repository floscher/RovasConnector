package app.rovas.josm.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
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
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

import app.rovas.josm.StaticConfig;
import app.rovas.josm.gen.PluginVersion;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public abstract class ApiQuery<EC extends ApiQuery.ErrorCode> {
  private static final Pattern POSITIVE_INT_PATTERN = Pattern.compile("^-?[0-9]+$");

  protected final UrlProvider urlProvider;
  protected final URL queryUrl;

  public ApiQuery(final UrlProvider urlProvider, final URL queryUrl) {
    this.urlProvider = urlProvider;
    this.queryUrl = queryUrl;
  }

  /**
   * @return the error codes that we know can happen for this endpoint
   */
  protected abstract EC[] getErrorCodes();

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
   * @throws ApiException.WrongPluginApiCredentials if the API credentials for the plugin -
   *   {@link StaticConfig#PLUGIN_API_KEY}/{@link StaticConfig#PLUGIN_API_TOKEN} - are not accepted by the server
   * @throws ApiException see the more specific descriptions of the subclasses
   */
  protected abstract int query(final ApiCredentials credentials) throws ApiException;

  /**
   * A wrapper for {@link #query(ApiCredentials)} that uses callbacks instead of throwing exceptions
   * @param credentials the user's credentials that are used for this request
   * @param successCallback in case a successful result was retrieved, the result will be passed to this consumer as a positive integer
   * @param errorCallback in case an error occurs, the error code will be passed to this consumer
   */
  public void query(
    final ApiCredentials credentials,
    final Consumer<Integer> successCallback,
    final Consumer<EC> errorCallback
  ) {
    try {
      final int result = query(credentials);
      final Optional<EC> errorCode = Stream.of(getErrorCodes())
        .filter(it -> it.getCode().map(code -> code == result).orElse(false))
        .findFirst();
      if (errorCode.isPresent()) {
        errorCallback.accept(errorCode.get());
      } else {
        final Optional<Integer> success = Optional.of(result).filter(it -> it > 0);
        if (success.isPresent()) {
          successCallback.accept(success.get());
        } else {
          errorCallback.accept(createAdditionalErrorCode(Optional.of(result), I18n.marktr("An unknown error occured!")));
        }
      }
    } catch (final ApiException e) {
      errorCallback.accept(createAdditionalErrorCode(Optional.empty(), e.getMessage()));
      if (e instanceof ApiException.WrongPluginApiCredentials) {
        BugReportQueue.getInstance().submit(new ReportedException(e));
      }
    }
  }

  protected URLConnection sendPostRequest(final JsonObjectBuilder requestContent) throws ApiException.ConnectionFailure {
    final URLConnection connection;
    try {
      connection = queryUrl.openConnection();
    } catch (final IOException e) {
      throw new ApiException.ConnectionFailure(queryUrl);
    }
    try {
      connection.setRequestProperty("API-KEY", StaticConfig.PLUGIN_API_KEY);
      connection.setRequestProperty("TOKEN", StaticConfig.PLUGIN_API_TOKEN);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("User-Agent", "JOSM-rovas/" + PluginVersion.versionName);
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
      try (final Writer writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
        writer.write(requestContent.build().toString());
      }
    } catch (IOException e) {
      disconnect(connection);
      throw new ApiException.ConnectionFailure(queryUrl);
    }
    return connection;
  }

  /**
   * Decodes JSON in the form {@code {"result": "42"}} from the given input stream.
   * @param connection the connection from which the response is read
   * @param key the JSON key to which the resulting value is mapped
   */
  protected static int decodeJsonResult(final URLConnection connection, final String key) throws ApiException {
    try {
      if (connection instanceof HttpURLConnection && HttpURLConnection.HTTP_UNAUTHORIZED == ((HttpURLConnection) connection).getResponseCode()) {
        throw new ApiException.WrongPluginApiCredentials(connection.getURL());
      }
      try (final InputStream stream = connection.getInputStream()) {
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
          ).orElseThrow(() -> new ApiException.DecodeResponse(connection.getURL()));
      }
    } catch (JsonException je) { // can be thrown by readObject()
      throw new ApiException.DecodeResponse(connection.getURL());
    } catch (IOException e) {
      throw new ApiException.ConnectionFailure(connection.getURL());
    } finally {
      disconnect(connection);
    }
  }

  private static void disconnect(final URLConnection connection) {
    Utils.instanceOfAndCast(connection, HttpURLConnection.class).ifPresent(HttpURLConnection::disconnect);
  }

  public static class ErrorCode {
    @NotNull
    private final Optional<Integer> code;
    @NotNull
    private final String translatableMessage;

    public ErrorCode(@NotNull final Optional<Integer> code, @NotNull final String translatableMessage) {
      this.code = Objects.requireNonNull(code);
      this.translatableMessage = Objects.requireNonNull(translatableMessage);
    }

    @NotNull
    public Optional<Integer> getCode() {
      return code;
    }

    @NotNull
    public String getTranslatableMessage() {
      return translatableMessage;
    }
  }
}
