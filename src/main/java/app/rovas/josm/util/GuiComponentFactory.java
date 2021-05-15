package app.rovas.josm.util;

import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.OpenBrowser;

public final class GuiComponentFactory {

  private GuiComponentFactory() {
    // private constructor to prevent instantiation
  }

  public static JMultilineLabel createHyperlinkedMultilineLabel(final String text) {
    final JMultilineLabel label = new JMultilineLabel(text);
    label.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        OpenBrowser.displayUrl(e.getURL().toExternalForm());
      }
    });
    return label;
  }

  public static JLabel createLabel() {
    return createLabel(null, false);
  }

  public static JLabel createLabel(final String text, final boolean bold) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
    return label;
  }
}
