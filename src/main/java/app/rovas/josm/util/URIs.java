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

  public static final URI USER_PROFILE = orNull(String.format("https://%s/user", ROVAS_DOMAIN));

  @Nullable
  public static URI orNull(@NotNull final String stringUri) {
    try {
      return new URI(stringUri);
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
