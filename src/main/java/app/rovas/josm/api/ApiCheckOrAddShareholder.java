package app.rovas.josm.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonString;

import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

import app.rovas.josm.gen.PluginVersion;
import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.URIs;

public final class ApiCheckOrAddShareholder {

  private static final Pattern POSITIVE_INT_PATTERN = Pattern.compile("^-?[0-9]+$");

  public static final int ERROR_CODE_SHAREHOLDER_PRIVATE = 0;
  public static final int ERROR_CODE_SHAREHOLDER_NOT_FOUND = -1;
  public static final int ERROR_CODE_SHAREHOLDER_UNAUTHORIZED = -2;

  /**
   *
   * @param credentials the credentials
   * @return a positive integer means success, it is the worker merit allocation ID.
   *   If it is not a positive integer, then:<ul>
   *     <li>0: the project is private, you can't become a shareholder</li>
   *     <li>-1: the project does not exist</li>
   *     <li>-2: no user was found for the given key and token (only happens if header is valid and body is invalid, otherwise a 401 is returned)</li>
   *     <li>anything lower would be an unknown error code from the server</li>
   *   </ul>
   * @throws ApiException.ConnectionFailure if any connection error occurs, so the response can not be read completely
   * @throws ApiException.DecodeResponse if the response was received, but can't be decoded
   */
  public static int query(final ApiCredentials credentials) throws ApiException.ConnectionFailure, ApiException.DecodeResponse {
    final URL url = URIs.getInstance().rules_checkOrAddShareholder();
    final URLConnection connection;
    try {
      connection = url.openConnection();
    } catch (final IOException e) {
      throw new ApiException.ConnectionFailure(url);
    }
    try {
      connection.setRequestProperty("API-KEY", credentials.getApiKey());
      connection.setRequestProperty("TOKEN", credentials.getApiToken());
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
        writer.write(
          Json.createObjectBuilder()
            .add("project_id", credentials.getProjectId())
            .add("api_key", credentials.getApiKey())
            .add("token", credentials.getApiToken())
            .build().toString()
        );
      }

      /*
       * When the HTTP headers have a wrong API key/token combination, we get a HTTP status 401 instead of a JSON response.
       * This is treated the same as if a JSON response with an `ERROR_CODE_SHAREHOLDER_UNAUTHORIZED` was returned.
       */
      if (
        connection instanceof HttpURLConnection &&
          ((HttpURLConnection) connection).getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED
      ) {
        // The server will only return -2, if the header has correct key/token combination, while the body has a wrong one.
        return ERROR_CODE_SHAREHOLDER_UNAUTHORIZED;
      } else {
        return decodeJsonResult(connection.getInputStream()).orElseThrow(() -> new ApiException.DecodeResponse(connection.getURL()));
      }
    } catch (IOException ioe) {
      throw new ApiException.ConnectionFailure(url);
    } finally {
      Utils.instanceOfAndCast(connection, HttpURLConnection.class).ifPresent(HttpURLConnection::disconnect);
    }
  }

  /**
   * Decodes JSON in the form {@code {"result": "42"}} from the given input stream.
   * @throws IOException if there are issues related to reading from the input stream
   * @return an Optional containing the integer result value.<br>
   *  It is empty, if the content of the input stream could be read, but the result value can't be extracted.
   */
  private static Optional<Integer> decodeJsonResult(final InputStream inputStream) throws IOException {
    try (final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return Optional.ofNullable(Json.createReader(reader).readObject())
        .flatMap(it ->
          Optional.ofNullable(it.get("result"))
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
        );
    } catch (JsonException je) { // can be thrown by readObject()
      return Optional.empty();
    }
  }

  public static void query(
    final ApiCredentials credentials,
    final Consumer<Integer> successCallback,
    final BiConsumer<String, Boolean> errorWithRetryCallback
  ) {
    try {
      final int result = query(credentials);
      switch (result) {
        case ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_PRIVATE:
          errorWithRetryCallback.accept(I18n.marktr("Could not access the chosen project!"), true);return;
        case ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_NOT_FOUND:
          errorWithRetryCallback.accept(I18n.marktr("Could not find any project with the given ID!"), true);return;
        case ApiCheckOrAddShareholder.ERROR_CODE_SHAREHOLDER_UNAUTHORIZED:
          errorWithRetryCallback.accept(I18n.marktr("Your API key and token seem to be invalid!"), true);return;
        default:
          if (result > 0) {
            successCallback.accept(result);
          } else {
            errorWithRetryCallback.accept(I18n.marktr("An unknown error occured!"), false);
          }
      }
    } catch (ApiException.DecodeResponse e) {
      errorWithRetryCallback.accept(I18n.marktr("There was an error decoding the server response!"), false);
    } catch (ApiException.ConnectionFailure e) {
      errorWithRetryCallback.accept(I18n.marktr("There was a connection issue!"), false);
    }
  }
}
