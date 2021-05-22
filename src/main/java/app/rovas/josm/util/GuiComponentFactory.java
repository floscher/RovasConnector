package app.rovas.josm.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicSpinnerUI;
import javax.swing.text.DefaultFormatter;

import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

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
    return createWrapperPanel(c, new GridLayout(1, 1));
  }

  public static JPanel createWrapperPanel(final Component c, final LayoutManager layoutManager) {
    final JPanel panel = new JPanel(layoutManager);
    panel.add(c);
    return panel;
  }

  public static JLabel createLabel(final String text, final boolean bold) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
    return label;
  }

  public static JSpinner createSpinner(final SpinnerModel model, final int columns, final boolean hideArrowButtons) {
    return createSpinner(model, columns, hideArrowButtons, "#");
  }

  public static JSpinner createSpinner(final SpinnerModel model, final int columns, final boolean hideArrowButtons, final String decimalFormatPattern) {
    final JSpinner spinner = new JSpinner(model);
    final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, decimalFormatPattern);

    // Commit valid changes immediately, so the change listeners are notified
    Utils.instanceOfAndCast(editor.getTextField().getFormatter(), DefaultFormatter.class).ifPresent(formatter -> formatter.setCommitsOnValidEdit(true));

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
