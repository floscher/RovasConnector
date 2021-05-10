package app.rovas.josm.util;

import javax.swing.event.HyperlinkEvent;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.OpenBrowser;

public class JMultilineLinkedLabel extends JMultilineLabel {
  public JMultilineLinkedLabel(final String text) {
    super(text);
    addHyperlinkListener((e) -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        OpenBrowser.displayUrl(e.getURL().toExternalForm());
      }
    });
  }
}
