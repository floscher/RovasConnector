package app.rovas.josm.util;

import org.openstreetmap.josm.tools.I18n;

public final class I18nStrings {

  public static String trShorthandForHours() {
    return I18n.trc("shorthand for hours", "h");
  }

  public static String trShorthandForMinutes() {
    return I18n.trc("shorthand for minutes", "m");
  }

  private I18nStrings() {
    // private constructor to prevent instantiation
  }

  public static String trVerificationWarningWithHyperlink() {
    return
      "<span style='color:#ff0000'>" +
      I18n.tr("<strong>Please note,</strong> by creating a work report you also agree to verify (typically) two work reports submitted by other users for each of your submitted work reports.") +
      "</span><br>" +
      I18n.tr(
        // i18n: {0} is replaced by a link labeled "rules page in Rovas"
        "See the {0} for more information",
        URIs.toHtmlHyperlink(URIs.rules(), I18n.tr("rules page in Rovas"))
      );
  }
}
