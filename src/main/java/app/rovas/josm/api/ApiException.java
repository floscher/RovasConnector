// License: GPL. For details, see LICENSE file.
package app.rovas.josm.api;

import java.net.URL;

import org.openstreetmap.josm.tools.I18n;

/**
 * An exception that happened when communicating with the API.
 * Use the appropriate subclass
 */
public abstract class ApiException extends Exception {

  private final URL url;
  private final String translatableMessage;
  private final boolean shouldBeReportedAsBug;

  /**
   * Creates a generic API exception
   * @param url the URL of the API endpoint at which the exception occured
   * @param cause the Exception that was the root cause
   * @param translatableMessage a message describing this exception, should be one that can be translated by {@link I18n#tr(String, Object...)}
   * @param shouldBeReportedAsBug if {@code true}, this exception should be reported as a bug to the maintainers of the plugin
   */
  protected ApiException(final URL url, final String translatableMessage, final boolean shouldBeReportedAsBug, final Throwable cause) {
    super(cause);
    this.url = url;
    this.translatableMessage = translatableMessage;
    this.shouldBeReportedAsBug = shouldBeReportedAsBug;
  }

  @Override
  public String getMessage() {
    return translatableMessage;
  }

  @Override
  public String getLocalizedMessage() {
    return I18n.tr(translatableMessage);
  }

  /**
   * @return the API URL that we were connected to when the exception occured
   */
  public URL getURL() {
    return this.url;
  }

  /**
   * @return {@code true} iff this exception should be filed as a JOSM bug by the user
   */
  public boolean isShouldBeReportedAsBug() {
    return shouldBeReportedAsBug;
  }

  /**
   * This exception should be thrown when some kind of connection problem occured (unexpected status code, unexpected disconnect, …).
   */
  public static class ConnectionFailure extends ApiException {
    /**
     * Creates an exception for connection problems with the API
     * @param url the URL of the endpoint that caused the exception
     * @param cause the exception that was the root cause
     */
    public ConnectionFailure(final URL url, final Throwable cause) {
      super(url, I18n.marktr("There was a connection issue!"), false, cause);
    }
  }

  /**
   * This exception should be thrown when we received a good response, but could not determine what the content means.
   */
  public static class DecodeResponse extends ApiException {
    /**
     * Creates an exception for when we can't decode the JSON response
     * @param url the URL of the endpoint that caused the exception
     * @param cause the exception that was the root cause
     */
    public DecodeResponse(final URL url, final Throwable cause) {
      super(url, I18n.marktr("There was an error decoding the server response!"), true, cause);
    }
  }

  /**
   * This exception should be thrown if we get a {@code 401} HTTP status code, meaning the API credentials of the user are invalid.
   */
  public static class WrongPluginApiCredentials extends ApiException {
    /**
     * Creates an exception for when the user credentials are not accepted by the server
     * @param url the URL of the endpoint at which the exception occured
     */
    public WrongPluginApiCredentials(final URL url) {
      super(
        url,
        I18n.marktr(
          "A work report could not be created, because the API key and/or token set in the preferences are incorrect, or your Rovas access was restricted. Please log into Rovas to verify your credentials and status."
        ),
        false,
        null
      );
    }
  }

}
