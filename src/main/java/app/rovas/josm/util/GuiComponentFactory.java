package app.rovas.josm.util;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.Arrays;
import java.util.Optional;
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

/**
 * Some utility methods for easy creation of GUI components like labels and spinners.
 */
public final class GuiComponentFactory {

  private GuiComponentFactory() {
    // private constructor to prevent instantiation
  }

  /**
   * Creates a {@link JMultilineLabel}, the text is directly passed to {@link JMultilineLabel#JMultilineLabel(String)}.
   * If there is a hyperlink (&lt;a href="…"&gt;&lt;/a&gt;) in the text , a browser will open if the user clicks the link.
   * @param text the text for the label, can contain HTML, surrounding it with &lt;html&gt;&lt;/html&gt; tags is not required
   * @return the created {@link JMultilineLabel}
   */
  public static JMultilineLabel createHyperlinkedMultilineLabel(final String text) {
    final JMultilineLabel label = new JMultilineLabel(text);
    label.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        OpenBrowser.displayUrl(e.getURL().toExternalForm());
      }
    });
    return label;
  }

  /**
   * Creates a {@link JPanel} that uses the given layout manager and all the given components are added in order
   * @param layoutManager the layout manager to use
   * @param components the components that are added to the wrapper panel
   * @return the wrapper panel
   */
  public static JPanel createWrapperPanel(final LayoutManager layoutManager, final Component... components) {
    final JPanel panel = new JPanel(layoutManager);
    Arrays.stream(components).forEach(panel::add);
    return panel;
  }

  /**
   * Creates a {@link JPanel} that has the given component as only child. That component fills the wrapper panel completely.
   * @param c the child component
   * @return the wrapper panel
   */
  public static JPanel createWrapperPanel(final Component c) {
    return createWrapperPanel(new GridLayout(1, 1), c);
  }

  /**
   * Creates a {@link JLabel}
   * @param text the text that is passed into {@link JLabel#JLabel(String)}
   * @param bold if {@code true}, the label is bold, otherwise it is a plain label
   * @return the created label
   */
  public static JLabel createLabel(final String text, final boolean bold) {
    return createLabel(text, bold, Optional.empty());
  }

  /**
   * Creates a {@link JLabel}
   * @param text the text that is passed into {@link JLabel#JLabel(String)}
   * @param bold if {@code true}, the label is bold, otherwise it is a plain label
   * @param horizontalAlignment the horizontal alignment (see {@link JLabel#setHorizontalAlignment(int)})
   * @return the created label
   */
  public static JLabel createLabel(final String text, final boolean bold, final int horizontalAlignment) {
    return createLabel(text, bold, Optional.of(horizontalAlignment));
  }

  private static JLabel createLabel(final String text, final boolean bold, final Optional<Integer> alignment) {
    final JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN));
    alignment.ifPresent(label::setHorizontalAlignment);
    return label;
  }

  /**
   * Creates a spinner
   * @param model the {@link SpinnerModel} used by the spinner
   * @param columns the number of columns (essentially how many digits fit in the textfield)
   * @param hideArrowButtons if {@link true}, the arrow buttons next to the text field are hidden.
   *   The value can still be adjusted by pressing the arrow keys on the keyboard.
   * @return the created spinner
   */
  public static JSpinner createSpinner(final SpinnerModel model, final int columns, final boolean hideArrowButtons) {
    return createSpinner(model, columns, hideArrowButtons, "#");
  }

  /**
   * Creates a spinner
   * @param model the {@link SpinnerModel} used by the spinner
   * @param columns the number of columns (essentially how many digits fit in the textfield)
   * @param hideArrowButtons if {@link true}, the arrow buttons next to the text field are hidden.
   *   The value can still be adjusted by pressing the arrow keys on the keyboard.
   * @param decimalFormatPattern the format pattern that should be used for the numbers
   *   (see {@link java.text.DecimalFormat#applyPattern(String)})
   * @return the created spinner
   */
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
