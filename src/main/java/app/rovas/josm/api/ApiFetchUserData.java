// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.net.URLConnection;
import java.util.Optional;
import javax.json.Json;

import com.drew.lang.annotations.NotNull;

import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.model.ApiCredentials;
import app.rovas.josm.util.UrlProvider;

public class ApiFetchUserData extends ApiQuery<UserData, ApiQuery.ErrorCode> {
  /**
   * Creates a new API query
   *
   * @param urlProvider the URL provider from which we can obtain URLs
   */
  public ApiFetchUserData(UrlProvider urlProvider) {
    super(urlProvider, urlProvider.rulesFetchUserData());
  }

  @NotNull
  @Override
  protected Optional<ErrorCode> getErrorCodeForResult(@NotNull final UserData result) {
    return Optional.empty();
  }

  @NotNull
  @Override
  protected ErrorCode getErrorCodeForException(@NotNull final ApiException exception) {
    return new ErrorCode(Optional.empty(), I18n.marktr("Error decoding user data!"));
  }

  @Override
  protected String getQueryLabel() {
    return "fetch user data";
  }

  @Override
  protected UserData query(ApiCredentials credentials) throws ApiException {
    final URLConnection connection = sendPostRequest(
      credentials,
      Json.createObjectBuilder()
    );
    return decodeJsonResult(connection, UserData::createFromJson);
  }
}
