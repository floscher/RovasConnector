package app.rovas.josm;

import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;

public final class RovasProperties {
  /**
   * The fraction of all earnings that goes to the Rovas connector project.
   * A value of 1.0 would be equal to 100%, 0.01 means 1%.
   * So for a value of {@code 0.01} from every 100 chrons earned, one of them would go to project {@link #ROVAS_CONNECTOR_PROJECT_ID}.
   */
  public static final double ASSET_USAGE_FEE = 0.01;
  /**
   * The project ID of the JOSM connector project in Rovas.
   */
  public static final Integer ROVAS_CONNECTOR_PROJECT_ID = 1998;

  public static final StringProperty ROVAS_API_KEY =
    new StringProperty("rovas.api-key", null);
  public static final StringProperty ROVAS_API_TOKEN =
    new StringProperty("rovas.api-token", null);
  public static final IntegerProperty ROVAS_ACTIVE_PROJECT_ID =
    new IntegerProperty("rovas.active-project-id", ROVAS_CONNECTOR_PROJECT_ID);

  private RovasProperties() {
    // private constructor to avoid instantiation
  }
}
