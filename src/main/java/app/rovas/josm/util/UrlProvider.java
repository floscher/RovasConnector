package app.rovas.josm.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.tools.JosmRuntimeException;

import app.rovas.josm.model.RovasProperties;

public class UrlProvider {

  private static final String BASE_URL_DEVELOPMENT = "https://dev.merit.world";
  private static final String BASE_URL_PRODUCTION = "https://rovas.app";

  private static final UrlProvider INSTANCE = new UrlProvider();

  public static UrlProvider getInstance() {
    return INSTANCE;
  }

  public static String toHtmlHyperlink(@Nullable final URL url, @NotNull final String label) {
    return url == null ? label : String.format("<a href=\"%s\">%s</a>", url, label);
  }

  protected UrlProvider() {
    // private constructor to prevent instantiation
  }
  private static URL uncheckedURL(final String path) {
    try {
      return new URL(path);
    } catch (MalformedURLException e) {
      throw new JosmRuntimeException("The rovas plugin builds broken URLs: " + path, e);
    }
  }

  protected String getBaseUrl() {
    return RovasProperties.DEVELOPER.get() ? BASE_URL_DEVELOPMENT : BASE_URL_PRODUCTION;
  }

  @NotNull
  public URL osmWikiPluginArticle() {
    return uncheckedURL("https://wiki.openstreetmap.org/wiki/JOSM/Plugins/RovasConnector");
  }

  @NotNull
  public URL node(final int id) {
    return uncheckedURL(getBaseUrl() + String.format("/node/%d", id));
  }

  @NotNull
  public URL rules() {
    return uncheckedURL(getBaseUrl() + "/rules");
  }

  @NotNull
  public URL userProfile() {
    return uncheckedURL(getBaseUrl() + "/user");
  }

  @NotNull
  public URL rulesCreateAUR() {
    return uncheckedURL(getBaseUrl() + "/rovas/rules/rules_proxy_create_aur");
  }

  @NotNull
  public URL rulesCheckOrAddShareholder() {
    return uncheckedURL(getBaseUrl() + "/rovas/rules/rules_proxy_check_or_add_shareholder");
  }

  @NotNull
  public URL rulesCreateWorkReport() {
    return uncheckedURL(getBaseUrl() + "/rovas/rules/rules_proxy_create_work_report");
  }
}
