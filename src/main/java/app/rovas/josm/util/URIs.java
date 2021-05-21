package app.rovas.josm.util;

import java.net.URI;
import java.net.URISyntaxException;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import app.rovas.josm.RovasProperties;

public final class URIs {
  public static String getRovasDomain() {
    return RovasProperties.DEVELOPER.get() ? "dev.merit.world" : "merit.world";
  }

  @Nullable
  public static URI project(final int id) {
    return orNull(String.format("https://%s/node/%d", getRovasDomain(), id));
  }

  public static URI rules() {
    return orNull(String.format("https://%s/rules", getRovasDomain()));
  }
  public static URI userProfile() {
    return orNull(String.format("https://%s/user", getRovasDomain()));
  }

  @Nullable
  public static URI orNull(@NotNull final String stringUri) {
    try {
      return new URI(stringUri);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  public static String toHtmlHyperlink(@Nullable final URI uri, @NotNull final String label) {
    return uri == null ? label : String.format("<a href=\"%s\">%s</a>", uri, label);
  }

  private URIs() {
    // private constructor to prevent instantiation
  }
}
