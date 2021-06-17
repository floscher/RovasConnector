// License: GPL. For details, see LICENSE file.
package app.rovas.josm.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

import org.openstreetmap.josm.tools.JosmRuntimeException;

import app.rovas.josm.model.RovasProperties;

/**
 * Supplies the URLs that we need, when opening a browser for the user, or making API requests.
 */
public class UrlProvider {
  private static final String BASE_URL_DEVELOPMENT = "https://dev.merit.world";
  private static final String BASE_URL_PRODUCTION = "https://rovas.app";

  private static final UrlProvider INSTANCE = new UrlProvider();

  private static final String PATH_API_CHECK_OR_ADD_SHAREHOLDER = "/rovas/rules/rules_proxy_check_or_add_shareholder";

  /**
   * @return the singleton instance of the {@link UrlProvider}.
   */
  public static UrlProvider getInstance() {
    return INSTANCE;
  }

  /**
   * Utility method to get a string containing the HTML for a hyperlink ({@code <a href=":url">:label</a>}})
   * @param url the url that is used for the link
   * @param label the label that is used for the link
   * @return the HTML of the link, as string
   */
  public static String toHtmlHyperlink(@Nullable final URL url, @NotNull final String label) {
    return url == null ? label : String.format("<a href=\"%s\">%s</a>", url, label);
  }

  /**
   * This is just visible for mocking in unit tests, use {@link #getInstance()} to obtain an instance.
    */
  @VisibleForTesting
  protected UrlProvider() {
    // only needed to be able to extend this class for testing
  }

  /**
   * Creates a {@link URL} instance without throwing a checked exception.
   * Use this only if you are sure that the URL is valid!
   * @param urlString the string representation of the URL
   * @return the {@link URL} object for the given string
   * @throws JosmRuntimeException in case the URL was malformed
   */
  public static URL uncheckedURL(final String urlString) {
    try {
      return new URL(urlString);
    } catch (MalformedURLException e) {
      throw new JosmRuntimeException("The rovas plugin builds broken URLs: " + urlString, e);
    }
  }

  /**
   * @return the base URL used for links to the Rovas app and API calls (includes domain and protocol, e.g. {@code https://rovas.app})
   */
  protected String getBaseUrl() {
    return RovasProperties.DEVELOPER.get() ? BASE_URL_DEVELOPMENT : BASE_URL_PRODUCTION;
  }

  /**
   * @param id the ID of the node (can be a project/report/…)
   * @return the URL {@code /node/:id} that points to the node that has the node ID specified as parameter
   */
  @NotNull
  public URL node(final int id) {
    return uncheckedURL(getBaseUrl() + String.format("/node/%d", id));
  }

  /**
   * @return the URL {@code /rules} on the Rovas website where the <a href="https://rovas.app/rules">economy rules can be found</a>.
   */
  @NotNull
  public URL rules() {
    return uncheckedURL(getBaseUrl() + "/rules");
  }

  /**
   * @return the URL {@code /user} on the Rovas website of the <a href="https://rovas.app/user">user profile</a> (prompts the user to login).
   */
  @NotNull
  public URL userProfile() {
    return uncheckedURL(getBaseUrl() + "/user");
  }

  /**
   * @return the URL of the <a href="https://merit.world/rovas-api#/rule/post_rovas_rules_rules_proxy_create_aur">
   *   API endpoint for creating the Asset Usage Report (AUR) for a work report</a>
   */
  @NotNull
  public URL rulesCreateAUR() {
    return uncheckedURL(getBaseUrl() + "/rovas/rules/rules_proxy_create_aur");
  }

  /**
   * @return the URL of the <a href="https://merit.world/rovas-api#/rule/post_rovas_rules_rules_proxy_check_or_add_shareholder">
   *   API endpoint for checking if a user is shareholder</a>
   */
  @NotNull
  public URL rulesCheckOrAddShareholder() {
    return uncheckedURL(getBaseUrl() + PATH_API_CHECK_OR_ADD_SHAREHOLDER);
  }

  /**
   * @return the URL of the <a href="https://merit.world/rovas-api#/rule/post_rovas_rules_rules_proxy_create_work_report">
   *   API endpoint for creating the work report itself</a>
   */
  @NotNull
  public URL rulesCreateWorkReport() {
    return uncheckedURL(getBaseUrl() + "/rovas/rules/rules_proxy_create_work_report");
  }
}
