package app.rovas.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.Test;

public class NonBlankTextFieldValidatorTest {

  @Test
  public void test() {
    final JTextField field = new JTextField();
    final MockValidator validator = new MockValidator(field);

    validator.assertCorrectFeedback(false, "");
    validator.assertCorrectFeedback(false, " \t\n\r ");
    validator.assertCorrectFeedback(true, "a");
  }

  private static class MockValidator extends NonBlankTextFieldValidator {

    private boolean expectValid;
    private boolean inAssertion = false;

    public MockValidator(JTextComponent component) {
      super(component);
    }

    public void assertCorrectFeedback(final boolean isValid, final String fieldContent) {
      getComponent().setText(fieldContent);
      this.expectValid = isValid;
      this.inAssertion = true;
      validate();
      this.inAssertion = false;
    }

    @Override
    protected void feedbackValid(String msg) {
      if (!inAssertion) {
        return;
      }
      assertTrue(expectValid, "Expected content to be invalid, but was valid!");
      assertEquals("", msg);
    }

    @Override
    protected void feedbackInvalid(final String msg) {
      if (!inAssertion) {
        return;
      }
      assertFalse(expectValid, "Expected content to be valid, but was valid!");
      assertFalse(msg.trim().isEmpty());
    }
  }
}
