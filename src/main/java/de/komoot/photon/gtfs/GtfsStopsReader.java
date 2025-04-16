/* (C)2025 */
package de.komoot.photon.gtfs;

import org.apache.commons.csv.CSVRecord;

public class GtfsStopsReader extends CsvBaseReader<GtfsStopResult> {

    public GtfsStopsReader(String dirName) {
        super(dirName, "/stops.txt");
    }

    @Override
    public GtfsStopResult handleRecord(CSVRecord record) {
        return new GtfsStopResult(record);
    }
}
