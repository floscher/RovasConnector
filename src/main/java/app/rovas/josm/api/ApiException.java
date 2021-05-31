package app.rovas.josm.api;

import java.net.URL;

import org.openstreetmap.josm.tools.I18n;

public abstract class ApiException extends Exception {

  private final URL url;
  private final String message;
  private final boolean shouldbeReportedAsBug;

  public ApiException(final URL url, final String message, final boolean shouldbeReportedAsBug) {
    this.url = url;
    this.message = message;
    this.shouldbeReportedAsBug = shouldbeReportedAsBug;
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

  public boolean shouldBeReportedAsBug() {
    return shouldbeReportedAsBug;
  }

  public static class ConnectionFailure extends ApiException {
    public ConnectionFailure(final URL url) {
      super(url, I18n.marktr("There was a connection issue!"), false);
    }
  }

  public static class DecodeResponse extends ApiException {
    public DecodeResponse(final URL url) {
      super(url, I18n.marktr("There was an error decoding the server response!"), false);
    }
  }

  public static class WrongPluginApiCredentials extends ApiException {
    public WrongPluginApiCredentials(final URL url) {
      super(url, I18n.marktr("The server did not accept the credentials of the plugin. Please report this to the maintainer of the Rovas plugin!"), true);
    }
  }

}
