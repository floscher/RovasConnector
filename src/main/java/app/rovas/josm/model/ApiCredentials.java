// License: GPL. For details, see LICENSE file.
package app.rovas.josm.model;

import java.util.Objects;
import java.util.Optional;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * An encapsulation for the API key and token of a user and the project ID for which the API calls should be executed.
 * This object ensures, that all three values are present (i.e. not-null).
 */
public class ApiCredentials {
  /**
   * The minimum value for a Rovas project ID that is considered a valid project ID.
   * Smaller values will be treated the same as if no value was set.
   */
  public static final int MIN_PROJECT_ID = 2;

  private final String apiKey;
  private final String apiToken;
  private final int projectId;

  /**
   * Creates credentials from its nullable parts. If any of the parts is null, or the project ID does not
   * satisfy {@link #isValidProjectId(Integer)}, then an empty {@link Optional} is returned.
   * Otherwise an optional with all three credentials is returned.
   * @param apiKey the API key or {@code null}
   * @param apiToken the API token or {@code null}
   * @param projectId the project ID or {@code null}
   * @return the API credentials, iff all three parameters are not-null, otherwise an empty {@link Optional}
   */
  public static Optional<ApiCredentials> createFrom(
    @Nullable final String apiKey,
    @Nullable final String apiToken,
    @Nullable final Integer projectId
  ) {
    return Optional.ofNullable(apiKey).flatMap(key ->
      Optional.ofNullable(apiToken).flatMap(token ->
        Optional.ofNullable(projectId)
          .filter(ApiCredentials::isValidProjectId)
          .map(id -> new ApiCredentials(key, token, id))
      )
    );
  }

  /**
   * Checks if the project ID is in the expected range (>= {@link #MIN_PROJECT_ID})
   * @param value the project ID to check
   * @return {@code true} iff the value is in the correct range
   */
  public static boolean isValidProjectId(final Integer value) {
    return value >= MIN_PROJECT_ID;
  }

  /**
   * Creates new API credentials
   * @param apiKey the API key to use (not-null)
   * @param apiToken the API token to use (not-null)
   * @param projectId the project ID to use (must satisfy {@link #isValidProjectId(Integer)})
   */
  public ApiCredentials(@NotNull final String apiKey, @NotNull final String apiToken, final int projectId) {
    CheckParameterUtil.ensureThat(
      ApiCredentials.isValidProjectId(projectId),
      () -> "Invalid Project ID (was " + projectId + ")!"
    );

    this.apiKey = Objects.requireNonNull(apiKey);
    this.apiToken = Objects.requireNonNull(apiToken);
    this.projectId = projectId;
  }

  /** @return the API key of the user */
  @NotNull
  public String getApiKey() {
    return apiKey;
  }

  /** @return the API token of the user */
  @NotNull
  public String getApiToken() {
    return apiToken;
  }

  /** @return the project ID for which the work report will be created */
  public int getProjectId() {
    return projectId;
  }
}
