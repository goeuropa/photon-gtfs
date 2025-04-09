/* (C)2025 */
package de.komoot.photon.gtfs;

import com.github.davidmoten.geo.GeoHash;
import de.komoot.photon.searcher.PhotonResult;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GtfsStopResult implements PhotonResult {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GtfsStopResult.class);

    private final String stopCode;
    private final String fullStopName;
    private final double stopLat;
    private final double stopLon;
    private final String geoHash;
    private double distance;

    private final String stopName;
    private final String cityName;
    private final String stopDesc;
    private final String locationType;
    private final String stopUrl;

    private final Map<String, String> extra;

    public GtfsStopResult(CSVRecord record) {

        stopCode = getOptionalValue(record, "stop_code");

        fullStopName = getRequiredValue(record, "stop_name");

        cityName = getCity();

        stopName = getName();

        stopDesc = getOptionalValue(record, "stop_desc");

        String stopLatStr = getRequiredValue(record, "stop_lat");
        stopLat = stopLatStr != null ? Double.parseDouble(stopLatStr) : Double.NaN;

        String stopLonStr = getRequiredValue(record, "stop_lon");
        stopLon = stopLonStr != null ? Double.parseDouble(stopLonStr) : Double.NaN;

        geoHash = GeoHash.encodeHash(stopLat, stopLon, 7);

        stopUrl = getOptionalValue(record, "stop_url");

        locationType = getOptionalValue(record, "location_type");

        extra = new HashMap<>();
        extra.put("description", this.stopDesc);
        extra.put("URL", this.stopUrl);
        extra.put("stop_code", String.valueOf(this.stopCode));
    }

    @Override
    public Object get(String key) {
        switch (key) {
            case "osm_key":
                return getKey(locationType);
            case "osm_value":
                return getType(locationType);
        }
        return null;
    }

    @Override
    public String getLocalised(String key, String language) {
        switch (key) {
            case "city":
                return cityName;
            case "name":
                return fullStopName;
        }
        return "";
    }

    @Override
    public Map<String, String> getMap(String key) {
        return extra;
    }

    @Override
    public double[] getCoordinates() {
        return new double[]{this.stopLon, this.stopLat};
    }

    @Override
    public double[] getExtent() {
        return null;
    }

    @Override
    public double getScore() {
        return 0;
    }

    @Override
    public JSONObject getRawData() {
        return new JSONObject();
    }

    private String getType(String locationType) {
        return locationType == null ? "bus_stop" : locationType.equals("0")
                ? "bus_stop" : "station";
    }

    private String getKey(String locationType) {
        return locationType == null ? "highway" : locationType.equals("0")
                ? "highway" : "railway";
    }

    private String getCity() {
        return fullStopName.contains(", ")
                ? fullStopName.split(", ")[0]
                : fullStopName.contains(",")
                ? fullStopName.split(",")[0]
                : null;
    }

    private String getName() {
        return fullStopName.contains(", ")
                ? fullStopName.split(", ")[1]
                : fullStopName.contains(",")
                ? fullStopName.split(",")[1]
                : fullStopName;
    }

    private String getRequiredValue(CSVRecord record, String name) {
        boolean required = true;
        return getValue(record, name, required);
    }

    private String getOptionalValue(CSVRecord record, String name) {
        boolean required = false;
        return getValue(record, name, required);
    }

    private String getValue(CSVRecord record, String name, boolean required) {

        if (!record.isSet(name)) {
            if (required) {
                LOGGER.error("Column {} not defined in file \"stops.txt\" yet it is required", name);
            }
            return null;
        }

        String value = record.get(name).trim();

        if (value.isEmpty()) {
            if (required) {
                LOGGER.error(
                        "For file \"stops.txt\" line number {} for column {} value was not set yet it is required",
                        (int) record.getRecordNumber(),
                        name);
            }
            return null;
        } else {

            return value.intern();
        }
    }

    public String getFullStopName() {
        return fullStopName;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public double getStopLon() {
        return stopLon;
    }

    public double getStopLat() {
        return stopLat;
    }

    public String getCityName() {
        return cityName;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        GtfsStopResult that = (GtfsStopResult) o;
        return Double.compare(stopLat, that.stopLat) == 0 && Double.compare(stopLon, that.stopLon) == 0 && Objects.equals(stopCode, that.stopCode) && Objects.equals(fullStopName, that.fullStopName) && Objects.equals(geoHash, that.geoHash) && Objects.equals(stopName, that.stopName) && Objects.equals(cityName, that.cityName) && Objects.equals(stopDesc, that.stopDesc) && Objects.equals(locationType, that.locationType) && Objects.equals(stopUrl, that.stopUrl);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(stopCode);
        result = 31 * result + Objects.hashCode(fullStopName);
        result = 31 * result + Double.hashCode(stopLat);
        result = 31 * result + Double.hashCode(stopLon);
        result = 31 * result + Objects.hashCode(geoHash);
        result = 31 * result + Objects.hashCode(stopName);
        result = 31 * result + Objects.hashCode(cityName);
        result = 31 * result + Objects.hashCode(stopDesc);
        result = 31 * result + Objects.hashCode(locationType);
        result = 31 * result + Objects.hashCode(stopUrl);
        return result;
    }

    @Override
    public String toString() {
        return "GtfsStopResult{" +
                "stopCode='" + stopCode + '\'' +
                ", fullStopName='" + fullStopName + '\'' +
                ", stopLat=" + stopLat +
                ", stopLon=" + stopLon +
                ", geoHash='" + geoHash + '\'' +
                ", stopUrl='" + stopUrl + '\'' +
                ", stopDesc='" + stopDesc + '\'' +
                '}';
    }
}
