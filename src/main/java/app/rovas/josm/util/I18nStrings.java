package app.rovas.josm.util;

import org.openstreetmap.josm.tools.I18n;

/**
 * A few translatable messages that are reused in several places of the GUI
 */
public final class I18nStrings {

  /**
   * @return the abbreviation for hours ({@code h} in English)
   */
  public static String trShorthandForHours() {
    return I18n.trc("shorthand for hours", "h");
  }

  /**
   * @return the abbreviation for minutes ({@code m} in English)
   */
  public static String trShorthandForMinutes() {
    return I18n.trc("shorthand for minutes", "m");
  }

  private I18nStrings() {
    // private constructor to prevent instantiation
  }

  /**
   * @return the translated message noting that users need to verify other work reports, uses HTML to display part of the message in red
   */
  public static String trVerificationWarningWithHyperlink() {
    return
      "<span style='color:#ff0000'>" +
      I18n.tr("<strong>Please note,</strong> by creating a work report you also agree to verify (typically) two work reports submitted by other users for each of your submitted work reports.") +
      "</span><br>" +
      I18n.tr(
        // i18n: {0} is replaced by a link labeled "rules page in Rovas"
        "See the {0} for more information",
        UrlProvider.toHtmlHyperlink(UrlProvider.getInstance().rules(), I18n.tr("rules page in Rovas"))
      );
  }
}
