package app.rovas.josm.util;

import java.net.URI;
import java.net.URISyntaxException;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

public final class URIs {
  public static final String ROVAS_DOMAIN = "dev.merit.world";

  @Nullable
  public static URI project(final int id) {
    return orNull(String.format("https://%s/node/%d", ROVAS_DOMAIN, id));
  }

  public static final URI RULES = orNull(String.format("https://%s/rules", ROVAS_DOMAIN));
  public static final URI USER_PROFILE = orNull(String.format("https://%s/user", ROVAS_DOMAIN));

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
