package app.rovas.josm.util;

import java.awt.GridBagConstraints;

import com.drew.lang.annotations.NotNull;

public final class GBCUtil {
  /**
   * Modifies GridBagConstraints, so that {@link java.awt.GridBagConstraints#gridx} is set to {@code columnIndex}.
   *
   * @param columnIndex the index of the column where the cell should be constrained to
   * @param gbc         the constraints that should be modified
   * @return the object that was passed as parameter {@code gbc}, modified to have {@code gridx} set to the value {@code columnIndex}.
   */
  @NotNull
  public static <G extends GridBagConstraints> G fixedToColumn(final int columnIndex, @NotNull final G gbc) {
    gbc.gridx = columnIndex;
    return gbc;
  }

  private GBCUtil() {
    // private constructor to prevent instantiation
  }
}
