package app.rovas.josm.api;

import java.net.URL;

import org.openstreetmap.josm.tools.I18n;

public abstract class ApiException extends Exception {

  private final URL url;
  private final String message;
  private final boolean shouldBeReportedAsBug;

  public ApiException(final URL url, final String message, final boolean shouldBeReportedAsBug, final Throwable cause) {
    super(cause);
    this.url = url;
    this.message = message;
    this.shouldBeReportedAsBug = shouldBeReportedAsBug;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getLocalizedMessage() {
    return I18n.tr(message);
  }

  public URL getURL() {
    return this.url;
  }

  public boolean isShouldBeReportedAsBug() {
    return shouldBeReportedAsBug;
  }

  public static class ConnectionFailure extends ApiException {
    public ConnectionFailure(final URL url, final Throwable cause) {
      super(url, I18n.marktr("There was a connection issue!"), false, cause);
    }
  }

  public static class DecodeResponse extends ApiException {
    public DecodeResponse(final URL url, final Throwable cause) {
      super(url, I18n.marktr("There was an error decoding the server response!"), true, cause);
    }
  }

  public static class WrongPluginApiCredentials extends ApiException {
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
