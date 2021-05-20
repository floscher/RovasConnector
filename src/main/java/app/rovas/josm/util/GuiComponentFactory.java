package app.rovas.josm.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicSpinnerUI;

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

  public static JPanel createWrapperPanel(final Component c) {
    final JPanel panel = new JPanel(new GridLayout(1, 1));
    panel.add(c);
    return panel;
  }

  public static JLabel createLabel(final String text, final boolean bold) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
    return label;
  }

  public static JSpinner createSpinner(final SpinnerModel model, final int columns) {
    return createSpinner(model, columns, false);
  }

  public static JSpinner createSpinner(final SpinnerModel model, final int columns, final boolean hideArrowButtons) {
    final JSpinner spinner = new JSpinner(model);
    final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
    editor.getTextField().setColumns(columns);
    spinner.setEditor(editor);
    if (hideArrowButtons) {
      spinner.setUI(new BasicSpinnerUI() {
        @Override
        protected Component createNextButton() {
          return null;
        }
        @Override
        protected Component createPreviousButton() {
          return null;
        }
      });
    }
    return spinner;
  }
}
