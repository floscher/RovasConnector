package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Optional;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public final class ApiCheckOrAddShareholder extends ApiQuery<ApiCheckOrAddShareholder.ErrorCode> {

  public static class ErrorCode extends ApiQuery.ErrorCode {
    public enum ContinueOption {
      RETRY_UPDATE_API_CREDENTIALS,
      RETRY_IMMEDIATELY
    }

    @NotNull
    private final ContinueOption continueOption;

    public ErrorCode(final Optional<Integer> code, @NotNull final String translatableMessage, @NotNull final ContinueOption continueOption) {
      super(code, translatableMessage);
      this.continueOption = continueOption;
    }

    @NotNull
    public ContinueOption getContinueOption() {
      return continueOption;
    }
  }

  @Override
  protected ErrorCode[] getErrorCodes() {
    return new ErrorCode[]{
      new ErrorCode(Optional.of(0), I18n.marktr("Could not access the chosen project!"), ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS),
      new ErrorCode(Optional.of(-1), I18n.marktr("Could not find any project with the given ID!"), ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS),
      new ErrorCode(Optional.of(-2), I18n.marktr("Your API key and token seem to be invalid!"), ErrorCode.ContinueOption.RETRY_UPDATE_API_CREDENTIALS),
    };
  }

  @Override
  protected ErrorCode createAdditionalErrorCode(final Optional<Integer> code, String translatableMessage) {
    return new ErrorCode(code, translatableMessage, ErrorCode.ContinueOption.RETRY_IMMEDIATELY);
  }

  public ApiCheckOrAddShareholder(final UrlProvider urlProvider) {
    super(urlProvider, urlProvider.rules_checkOrAddShareholder());
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
      Json.createObjectBuilder()
        .add("project_id", credentials.getProjectId())
        .add("api_key", credentials.getApiKey())
        .add("token", credentials.getApiToken())
    );
    return decodeJsonResult(connection, "result");
  }
}
