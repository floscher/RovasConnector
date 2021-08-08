// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Optional;
import java.util.stream.Stream;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.JsonUtil;
import app.rovas.josm.util.UrlProvider;

/**
 * The API call that checks if the user is a shareholder or can be made a shareholder.
 * See <a href="https://merit.world/rovas-api#/rule/post_rovas_rules_rules_proxy_check_or_add_shareholder">
 * the API documentation for this endpoint</a>.
 */
public final class ApiCheckOrAddShareholder extends ApiQuery<Integer, ApiQuery.ErrorCode> {
  private static final ErrorCode[] KNOWN_CODES = new ErrorCode[]{
    new ErrorCode(
      Optional.of(0),
      I18n.marktr("You could not have been made a shareholder of the project with the ID set in the preferences.")
    ),
    new ErrorCode(
      Optional.of(-1),
      I18n.marktr("Could not find any project with the project ID that is set in the preferences!")
    ),
  };

  /**
   * Creates the API query.
   * @param urlProvider the URL provider, from which the URL is retrieved using {@link UrlProvider#rulesCheckOrAddShareholder()}.
   */
  public ApiCheckOrAddShareholder(final UrlProvider urlProvider) {
    super(urlProvider, urlProvider.rulesCheckOrAddShareholder());
  }

  @NotNull
  @Override
  protected Optional<ErrorCode> getErrorCodeForResult(@NotNull Integer result) {
    if (result > 0) {
      return Optional.empty();
    }
    return Optional.of(
      Stream.of(KNOWN_CODES)
        .filter(it -> it.getCode().isPresent() && it.getCode().get().equals(result))
        .findFirst()
        .orElse(new ErrorCode(Optional.of(result), I18n.marktr("An unknown error occured (code=" + result + ")!")))
    );
  }

  @NotNull
  @Override
  protected ErrorCode getErrorCodeForException(@NotNull ApiException exception) {
    return new ErrorCode(Optional.empty(), exception.getLocalizedMessage());
  }

  @Override
  protected String getQueryLabel() {
    return "check or add shareholder";
  }

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
  @NotNull
  @Override
  public Integer query(final ApiCredentials credentials) throws ApiException {
    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder().add("project_id", credentials.getProjectId())
    );
    return decodeJsonResult(connection, it -> JsonUtil.extractResponseCode(it, "result"));
  }
}
