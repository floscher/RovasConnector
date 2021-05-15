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
  /**
   * The number of seconds of inactivity after each edit, which should still be counted.
   *
   * For a value of 30 this means: If you make only one edit, automatically 30 seconds are counted.
   * If you make one edit and after 15 seconds another one, then 45 seconds are counted, because the clock is
   * stopped 30 seconds after the second edit.
   * If you make one edit and another one after 60 seconds, then 60 seconds are counted, because 30 seconds
   * after the first edit the clock is stopped and also 30 seconds after the second edit.
   */
  public static final IntegerProperty INACTIVITY_TOLERANCE_SECONDS =
    new IntegerProperty("rovas.inactivity-tolerance-seconds", 30);

  private RovasProperties() {
    // private constructor to avoid instantiation
  }
}
