package app.rovas.josm;

import java.util.Optional;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public final class RovasPreference extends DefaultTabPreferenceSetting {

  private final RovasPreferencePanel prefPanel = new RovasPreferencePanel();

  public RovasPreference() {
    super("rovas_logo", I18n.tr("Rovas"), I18n.tr("Preferences to configure timetracking with Rovas"));
  }

  @Override
  public void addGui(PreferenceTabbedPane gui) {
    prefPanel.setApiKeyValue(RovasProperties.ROVAS_API_KEY.get());
    prefPanel.setApiTokenValue(RovasProperties.ROVAS_API_TOKEN.get());
    prefPanel.setActiveProjectIdValue(RovasProperties.ROVAS_ACTIVE_PROJECT_ID.get());
    gui.createPreferenceTab(this).add(prefPanel, GBC.eol().fill());
  }

  @Override
  public boolean ok() {
    RovasProperties.ROVAS_API_KEY.put(prefPanel.getApiKeyValue());
    RovasProperties.ROVAS_API_TOKEN.put(prefPanel.getApiTokenValue());
    RovasProperties.ROVAS_ACTIVE_PROJECT_ID.put(prefPanel.getActiveProjectIdValue());
    return false; // no restart required
  }
}
