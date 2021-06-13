package app.rovas.josm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UrlProviderTest {

  @Test
  public void testUrls() {
    assertEquals("https://rovas.app/node/42", UrlProvider.getInstance().node(42).toString());
    assertEquals("https://rovas.app/node/-73", UrlProvider.getInstance().node(-73).toString());
  }
}
