package app.rovas.josm.api;

import java.net.URL;

public abstract class ApiException extends Exception {

  private final URL url;

  public ApiException(final URL url) {
    this.url = url;
  }

  public URL getURL() {
    return this.url;
  }

  public static class ConnectionFailure extends ApiException {
    public ConnectionFailure(URL url) {
      super(url);
    }
  }

  public static class DecodeResponse extends ApiException {
    public DecodeResponse(URL url) {
      super(url);
    }
  }

}
