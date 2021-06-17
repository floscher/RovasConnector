# Rovas Connector plugin

This JOSM plugin allows tracking the time spent editing OSM data in JOSM and then reporting that time
to https://rovas.app

## Development

Clone this repository, make sure you have JDK 8 or newer installed.

Then you can use these commands to build:
```bash
# Start a JOSM instance for development with the current state of the plugin loaded
./gradlew runJosm
# Run unit tests
./gradlew test
# Run a full build
./gradlew build
```

## Translations

The translation of the plugin is happening at [Transifex](https://www.transifex.com/josm/josm/josm-plugin_RovasConnector/):

[![translation stats](https://www.transifex.com/projects/p/josm/resource/josm-plugin_RovasConnector/chart/image_png/)](https://www.transifex.com/josm/josm/josm-plugin_RovasConnector/)

In order to update the translations in this repository with the current translations from Transifex, [install the Transifex client](https://docs.transifex.com/client/installing-the-client) and then run this command:
```bash
./gradlew transifexDownload
```
