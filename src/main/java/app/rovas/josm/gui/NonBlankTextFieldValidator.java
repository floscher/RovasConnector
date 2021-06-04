package app.rovas.josm.gui;

import java.util.Optional;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;

public class NonBlankTextFieldValidator extends AbstractTextComponentValidator {

  public NonBlankTextFieldValidator(final JTextComponent component) {
    super(component);
  }

  @Override
  public void validate() {
    final boolean isValid = isValid();
    if (isValid) {
      feedbackValid("");
    } else {
      feedbackInvalid("This field has to be filled out!");
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
