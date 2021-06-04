package app.rovas.josm.model;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

import app.rovas.josm.gui.RovasPreferencePanel;
import app.rovas.josm.util.RovasProperties;

public final class RovasPreference extends DefaultTabPreferenceSetting {

  private final RovasPreferencePanel prefPanel = new RovasPreferencePanel();

  public RovasPreference() {
    super("rovas_logo", I18n.tr("Rovas"), I18n.tr("Preferences to configure timetracking with Rovas"));
  }

  @Override
  public void addGui(final PreferenceTabbedPane gui) {
    prefPanel.setApiKeyValue(RovasProperties.ROVAS_API_KEY.get());
    prefPanel.setApiTokenValue(RovasProperties.ROVAS_API_TOKEN.get());
    prefPanel.setActiveProjectIdValue(RovasProperties.ACTIVE_PROJECT_ID.get());
    prefPanel.setInactivityTolerance(RovasProperties.INACTIVITY_TOLERANCE.get());
    gui.createPreferenceTab(this).add(prefPanel, GBC.eol().fill());
  }

  @Override
  public boolean ok() {
    RovasProperties.persistApiCredentials(prefPanel); // API key and token, active project ID
    RovasProperties.INACTIVITY_TOLERANCE.put(prefPanel.getInactivityTolerance());
    return false; // no restart required
  }
}
