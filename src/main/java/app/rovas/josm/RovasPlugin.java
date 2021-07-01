// License: GPL. For details, see LICENSE file.
package app.rovas.josm;

import java.util.Optional;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

import app.rovas.josm.gui.CreateRovasReportDialog;
import app.rovas.josm.gui.RovasConnectorDialog;
import app.rovas.josm.model.AnyOsmDataChangeTracker;
import app.rovas.josm.model.RovasPreference;
import app.rovas.josm.model.RovasProperties;
import app.rovas.josm.model.TimeTrackingManager;

/**
 * <p>JOSM plugin that allows time tracking with https://rovas.app
 * Copyright (C) 2021 Florian Schäfer (floscher)</p>
 *
 * <p>This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.</p>
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.</p>
 *
 * <p>You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <a href="https://www.gnu.org/licenses/gpl-3.0">gnu.org</a>.</p>
 *
 * <hr>
 *
 * The main class of the rovas plugin.
 */
public final class RovasPlugin extends Plugin {
  public static final ImageProvider LOGO = new ImageProvider("rovas_logo");

  private final PreferenceSetting preference = new RovasPreference();
  private final TimeTrackingManager timeTrackingManager = new TimeTrackingManager();

  /**
   * Creates the plugin
   *
   * @param info the plugin information describing the plugin.
   */
  public RovasPlugin(final PluginInformation info) {
    super(info);
    MainApplication.getLayerManager().addAndFireLayerChangeListener(new AnyOsmDataChangeTracker(timeTrackingManager));
    timeTrackingManager.trackChangeNow();

    OsmServerWriter.registerPostprocessor((__, ___) -> {
      final Optional<Changeset> changeset = Optional.ofNullable(OsmApi.getOsmApi()).map(OsmApi::getChangeset);
      new Thread(() -> {
        if (RovasProperties.UNPAID_EDITOR.get()) {
          new CreateRovasReportDialog(
            timeTrackingManager,
            changeset,
            timeTrackingManager.commit()
          );
        } else {
          new Notification(I18n.tr("A Rovas work report can not be created, as your work is paid by a company. The setting can be changed in the Rovas Connector plugin preferences."))
            .setIcon(JOptionPane.INFORMATION_MESSAGE)
            .show();
          // Reset the time tracker when submitting a changeset, for which a company paid
          timeTrackingManager.setCurrentlyTrackedSeconds(0);
        }
      }).start();
    });
  }

  @Override
  public void mapFrameInitialized(final MapFrame oldFrame, final MapFrame newFrame) {
    super.mapFrameInitialized(oldFrame, newFrame);
    if (newFrame != null && newFrame.getToggleDialog(RovasConnectorDialog.class) == null) {
      newFrame.addToggleDialog(new RovasConnectorDialog(timeTrackingManager));
    }
  }

  @Override
  public PreferenceSetting getPreferenceSetting() {
    return preference;
  }
}
