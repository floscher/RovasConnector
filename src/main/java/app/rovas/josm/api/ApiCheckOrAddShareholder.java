package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Optional;
import javax.json.Json;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public final class ApiCheckOrAddShareholder extends ApiQuery<ApiQuery.ErrorCode> {
  @Override
  protected ErrorCode[] getKnownErrorCodes() {
    return new ErrorCode[]{
      new ErrorCode(
        Optional.of(0),
        I18n.marktr("You could not have been made a shareholder of the project with the ID set in the preferences.")
      ),
      new ErrorCode(
        Optional.of(-1),
        I18n.marktr("Could not find any project with the project ID that is set in the preferences!")
      ),
    };
  }

  @Override
  protected ErrorCode createAdditionalErrorCode(final Optional<Integer> code, final String translatableMessage) {
    return new ErrorCode(code, translatableMessage);
  }

  public ApiCheckOrAddShareholder(final UrlProvider urlProvider) {
    super(urlProvider, urlProvider.rulesCheckOrAddShareholder());
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
  @Override
  public int query(final ApiCredentials credentials) throws ApiException {
    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder().add("project_id", credentials.getProjectId())
    );
    return decodeJsonResult(connection, "result");
  }
}
