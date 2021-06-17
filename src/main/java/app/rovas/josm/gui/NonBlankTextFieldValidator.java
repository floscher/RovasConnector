// License: GPL. For details, see LICENSE file.
package app.rovas.josm.gui;

import java.util.Optional;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.tools.I18n;

/**
 * A validator that reports its textfield as invalid, if it contains the empty string or only
 * whitespace characters (as defined by {@link String#trim()}).
 */
public class NonBlankTextFieldValidator extends AbstractTextComponentValidator {

  /**
   * Creates a validator for the given component
   * @param component the text component that should be checked to not contain only whitespace or nothing at all
   */
  public NonBlankTextFieldValidator(final JTextComponent component) {
    super(component);
  }

  @Override
  public void validate() {
    final boolean isValid = isValid();
    if (isValid) {
      feedbackValid("");
    } else {
      feedbackInvalid(I18n.tr("This field has to be filled out!"));
    }
  }

  @Override
  public boolean isValid() {
    return !Optional.ofNullable(getComponent().getText())
      .map(String::trim)
      .map(String::isEmpty)
      .orElse(false);
  }
}
