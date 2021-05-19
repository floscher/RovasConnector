package app.rovas.josm;

import java.util.Optional;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;

import app.rovas.josm.util.NullableProperty;

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

  public static final NullableProperty<String> ROVAS_API_KEY = new NullableProperty<>(
    new StringProperty("rovas.api-key", null),
    it -> !it.trim().isEmpty()
  );
  public static final NullableProperty<String> ROVAS_API_TOKEN = new NullableProperty<>(
    new StringProperty("rovas.api-token", null),
    it -> !it.trim().isEmpty()
  );

  /**
   * When a value smaller than {@link #ACTIVE_PROJECT_ID_MIN_VALUE} is set for {@link #ACTIVE_PROJECT_ID},
   * then the value is replaced by this value, which is equivalent to no value being set.
   */
  public static final int ACTIVE_PROJECT_ID_NO_VALUE = -1;
  /**
   * The minimum value for {@link #ACTIVE_PROJECT_ID} that is considered a valid project ID.
   * Smaller values are allowed, though. These smaller values are all treated as "no value"
   * (see {@link #ACTIVE_PROJECT_ID_NO_VALUE}).
   */
  public static final int ACTIVE_PROJECT_ID_MIN_VALUE = 2;
  /**
   * The Rovas project ID for which the JOSM user is submitting their work report.
   */
  public static final IntegerProperty ACTIVE_PROJECT_ID =
    new IntegerProperty("rovas.active-project-id", ROVAS_CONNECTOR_PROJECT_ID);

  /**
   * @return {@code true} iff {@link #ACTIVE_PROJECT_ID} has a value greater than or equal to {@link #ACTIVE_PROJECT_ID_MIN_VALUE}
   */
  public static boolean isActiveProjectIdSet() {
    return Optional.ofNullable(ACTIVE_PROJECT_ID.get()).orElse(ACTIVE_PROJECT_ID_NO_VALUE) >= ACTIVE_PROJECT_ID_MIN_VALUE;
  }

  /**
   * The minimum value allowed for {@link #INACTIVITY_TOLERANCE}, smaller values are treated as if this value was set.
   */
  public static final int INACTIVITY_TOLERANCE_MIN_VALUE = 1;
  /**
   * The default value for {@link #INACTIVITY_TOLERANCE}.
   */
  public static final int INACTIVITY_TOLERANCE_DEFAULT_VALUE = 30;
  /**
   * The maximum value allowed for {@link #INACTIVITY_TOLERANCE}, larger values are treated as if this value was set.
   */
  public static final int INACTIVITY_TOLERANCE_MAX_VALUE = 240;

  /**
   * The number of seconds of inactivity after each edit, which should still be counted.
   *
   * For a value of 30 this means: If you make only one edit, automatically 30 seconds are counted.
   * If you make one edit and after 15 seconds another one, then 45 seconds are counted, because the clock is
   * stopped 30 seconds after the second edit.
   * If you make one edit and another one after 60 seconds, then 60 seconds are counted, because 30 seconds
   * after the first edit the clock is stopped and also 30 seconds after the second edit.
   */
  public static final IntegerProperty INACTIVITY_TOLERANCE =
    new IntegerProperty("rovas.inactivity-tolerance-seconds", INACTIVITY_TOLERANCE_DEFAULT_VALUE);

  /**
   * This property persists the time that was already tracked across restarts
   */
  public static final IntegerProperty ALREADY_TRACKED_TIME =
    new IntegerProperty("rovas.already-tracked-time", 0);
  public static final BooleanProperty ALWAYS_CREATE_REPORT = new BooleanProperty("rovas.always-create-report", false);

  private RovasProperties() {
    // private constructor to avoid instantiation
  }
}
