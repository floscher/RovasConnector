package app.rovas.josm.model;

import java.util.Objects;
import java.util.Optional;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.tools.CheckParameterUtil;

public class ApiCredentials {
  /**
   * The minimum value for a Rovas project ID that is considered a valid project ID.
   * Smaller values will be treated the same as if no value was set.
   */
  public static final int MIN_PROJECT_ID = 2;

  private final String apiKey;
  private final String apiToken;
  private final int projectId;

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

  public static boolean isValidProjectId(final Integer value) {
    return value >= MIN_PROJECT_ID;
  }

  public ApiCredentials(@NotNull final String apiKey, @NotNull final String apiToken, final int projectId) {
    CheckParameterUtil.ensureThat(
      ApiCredentials.isValidProjectId(projectId),
      () -> "Invalid Project ID (was " + projectId + ")!"
    );

    this.apiKey = Objects.requireNonNull(apiKey);
    this.apiToken = Objects.requireNonNull(apiToken);
    this.projectId = projectId;
  }

  @NotNull
  public String getApiKey() {
    return apiKey;
  }

  @NotNull
  public String getApiToken() {
    return apiToken;
  }

  public int getProjectId() {
    return projectId;
  }
}
