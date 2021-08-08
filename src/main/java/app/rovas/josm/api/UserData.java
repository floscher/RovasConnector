// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.util.Objects;
import java.util.Optional;
import javax.json.JsonObject;

import com.drew.lang.annotations.NotNull;

public class UserData {
  @NotNull
  public String getCountry() {
    return country;
  }

  public int getUid() {
    return uid;
  }

  @NotNull
  public String getUsername() {
    return username;
  }

  @NotNull
  public String getLangCode() {
    return langCode;
  }

  @NotNull
  public String getEmail() {
    return email;
  }

  public int getComplianceScore() {
    return complianceScore;
  }

  @NotNull
  public String getWholeName() {
    return wholeName;
  }

  @NotNull
  private final String country;
  private final int uid;
  @NotNull
  private final String username;
  @NotNull
  private final String langCode;
  @NotNull
  private final String email;
  private final int complianceScore;
  @NotNull
  private final String wholeName;

  public UserData(
    @NotNull final String country,
    final int uid,
    @NotNull final String username,
    @NotNull final String langCode,
    @NotNull final String email,
    @NotNull final String complianceScore,
    @NotNull final String wholeName
  ) throws NumberFormatException {
    this.country = Objects.requireNonNull(country);
    this.uid = uid;
    this.username = Objects.requireNonNull(username);
    this.langCode = Objects.requireNonNull(langCode);
    this.email = Objects.requireNonNull(email);
    this.complianceScore = Integer.parseInt(Objects.requireNonNull(complianceScore));
    this.wholeName = Objects.requireNonNull(wholeName);
  }

  public static Optional<UserData> createFromJson(final JsonObject json) {
    try {
      return Optional.of(
        new UserData(
          json.getString("country"),
          json.getInt("uid"),
          json.getString("username"),
          json.getString("lang_code"),
          json.getString("email"),
          json.getString("comp_score"),
          json.getString("whole_name")
        )
      );
    } catch (final ClassCastException | NullPointerException | NumberFormatException e) {
      return Optional.empty();
    }
  }

  @Override
  public String toString() {
    return "UserData{" +
      "country='" + country + '\'' +
      ", uid=" + uid +
      ", username='" + username + '\'' +
      ", langCode='" + langCode + '\'' +
      ", email='" + email + '\'' +
      ", complianceScore=" + complianceScore +
      ", wholeName='" + wholeName + '\'' +
      '}';
  }
}
