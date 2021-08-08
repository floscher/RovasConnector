// License: GPL. For details, see LICENSE file.
package app.rovas.josm.model;

/**
 * Configuration options that can't be adjusted by the user, but which are set by the plugin maintainer
 * and shipped with the plugin release.
 */
public final class StaticConfig {
  /**
   * The classification ID that determines what kind of work is reported.
   */
  public static final int NACE_CLASSIFICATION = 1645;
  /**
   * The fraction of all earnings that goes to the Rovas connector project.
   * A value of 1.0 would be equal to 100%, 0.01 means 1%.
   * So for a value of {@code 0.01} from every 100 chrons earned, one of them would go to project {@link #ROVAS_CONNECTOR_PROJECT_ID}.
   */
  public static final double ASSET_USAGE_FEE = 0.03;
  /**
   * The project ID of the JOSM connector project in Rovas.
   */
  public static final int ROVAS_CONNECTOR_PROJECT_ID = 35_259;
  /**
   * The project ID of the JOSM connector project on the dev server.
   */
  public static final int ROVAS_CONNECTOR_PROJECT_ID_DEV = 24_682;
  public static final int ROVAS_OSM_PROJECT_ID = 1998;
  public static final String ROVAS_CONNECTOR_PROJECT_URL = "https://rovas.app/josm_rovas_connector";
  /**
   * The address at which the edits can be seen.<br>
   * This <strong>must</strong> contain {@code %d} exactly one time. That will be replaced by the changeset ID.<br>
   * Any other percent signs that occur in the URL would have to be encoded as two percent signs ({@code %%}).<br>
   * @see java.util.Formatter
   */
  public static final String ROVAS_PROOF_URL = "https://overpass-api.de/achavi/?changeset=%d";

  private StaticConfig() {
    // private constructor to prevent instantiation
  }
}
