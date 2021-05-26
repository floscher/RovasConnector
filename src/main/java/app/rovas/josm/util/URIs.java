package app.rovas.josm.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

public final class URIs {

  private static final URIs INSTANCE = new URIs();

  public static URIs getInstance() {
    return INSTANCE;
  }

  public static String toHtmlHyperlink(@Nullable final URL url, @NotNull final String label) {
    return url == null ? label : String.format("<a href=\"%s\">%s</a>", url, label);
  }

  private final String domain = RovasProperties.DEVELOPER.get() ? "https://dev.merit.world" : "https://rovas.app";

  private URIs() {
    // private constructor to prevent instantiation
  }
  private URL uncheckedURL(final String path) {
    try {
      return new URL(domain + path);
    } catch (MalformedURLException e) {
      throw new RuntimeException("The rovas plugin builds broken URLs: " + domain + path, e);
    }
  }

  @NotNull
  public URL node(final int id) {
    return uncheckedURL(String.format("/node/%d", id));
  }

  @NotNull
  public URL rules() {
    return uncheckedURL("/rules");
  }

  @NotNull
  public URL userProfile() {
    return uncheckedURL("/user");
  }

  @NotNull
  public URL rules_createAUR() {
    return uncheckedURL("/rovas/rules/rules_proxy_create_aur");
  }

  @NotNull
  public URL rules_checkOrAddShareholder() {
    return uncheckedURL("/rovas/rules/rules_proxy_check_or_add_shareholder");
  }

  @NotNull
  public URL rules_createWorkReport() {
    return uncheckedURL("/rovas/rules/rules_proxy_create_work_report");
  }
}
