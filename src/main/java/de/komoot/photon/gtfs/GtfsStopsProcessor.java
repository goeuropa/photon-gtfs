/* (C)2025 */
package de.komoot.photon.gtfs;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class GtfsStopsProcessor {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GtfsStopsProcessor.class);
    private static final GtfsStopsProcessor instance = new GtfsStopsProcessor();

    private final List<GtfsStopResult> stopList;
    private final Map<String, List<GtfsStopResult>> geoIndexes;
    private final Map<String, List<GtfsStopResult>> gtfsStopsInGroups;

    private GtfsStopsProcessor() {
        GtfsStopsReader stopsReader = new GtfsStopsReader(getPath());
        stopList = Collections.unmodifiableList(stopsReader.get());
        geoIndexes = new HashMap<>(stopList.size());
        gtfsStopsInGroups = new HashMap<>();
        this.computeIndexesAndGroupByCityName();
    }

    public static GtfsStopsProcessor getProcessor() {
        return instance;
    }

    public List<GtfsStopResult> getStops() {
        return stopList;
    }

    public Map<String, List<GtfsStopResult>> getInGroups() {
        return gtfsStopsInGroups;
    }

    public Map<String, List<GtfsStopResult>> getIndexes() {
        return geoIndexes;
    }

    private String getPath() {
        Path root = Paths.get(".");
        String targetDirName = "photon_data";

        File gtfsPath = null;
        try {
            Optional<Path> found = Files.walk(root)
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals(targetDirName))
                    .findFirst();
            if (found.isPresent()) gtfsPath = new File(String.format("%s/gtfs", found.get()));
            else throw new FileNotFoundException("Could not find gtfs directory");
        } catch (IOException e) {
            LOGGER.warn("Something went wrong when trying to read the gtfs path: ", e);
        }

        if (gtfsPath.isDirectory()) {
            LOGGER.debug("Gtfs directory is found: {}", gtfsPath.getAbsolutePath());
            return gtfsPath.getAbsolutePath();
        } else {
            LOGGER.warn("Folder not found: /{}", targetDirName);
            return null;
        }
    }

    private void computeIndexesAndGroupByCityName() {
        stopList.forEach(stop -> {
            geoIndexes.computeIfAbsent(
                    stop.getGeoHash(),
                    key -> new ArrayList<>()).add(stop);
            String cityName = stop.getCityName() != null
                    ? stop.getCityName().toLowerCase()
                    : stop.getCityName();
            gtfsStopsInGroups.computeIfAbsent(
                    cityName,
                    key -> new ArrayList<>()).add(stop);
        });
    }
}
