package de.komoot.photon.gtfs;

import com.github.davidmoten.geo.GeoHash;
import de.komoot.photon.query.PhotonRequest;
import de.komoot.photon.query.ReverseRequest;
import de.komoot.photon.searcher.PhotonResult;

import java.util.*;
import java.util.stream.Collectors;

public class GtfsSearchStops {

    private static final GtfsStopsProcessor gtfsStops = GtfsStopsProcessor.getProcessor();
    private static final boolean isAbleToSearch = !gtfsStops.getStops().isEmpty();
    private static final int PRECISION_HASH_LENGTH = 7;

    public static boolean isAbleToSearch() {
        return isAbleToSearch;
    }

    public static List<PhotonResult> search(List<PhotonResult> results, PhotonRequest request) {
        List<PhotonResult> foundStops = new ArrayList<>(searchByName(request));
        if (!foundStops.isEmpty()) {
            foundStops.addAll(results);
            return foundStops;
        }
        foundStops.addAll(searchNearbyCoordinatesOfResults(results, request));
        foundStops.addAll(results);
        return foundStops;
    }

    public static List<PhotonResult> searchByLatLon(List<PhotonResult> results, ReverseRequest request) {
        Set<GtfsStopResult> foundStops = new TreeSet<>(Comparator.comparingDouble(GtfsStopResult::getDistance));

        double lat = request.getLocation().getY();
        double lon = request.getLocation().getX();

        String hash = GeoHash.encodeHash(lat, lon, PRECISION_HASH_LENGTH);
        List<String> neighbors = GeoHash.neighbours(hash);
        Set<String> area = new HashSet<>(neighbors);
        area.add(hash);

        area.stream().map(nearbyHash -> gtfsStops.getIndexes().getOrDefault(nearbyHash, List.of()))
                .flatMap(Collection::stream)
                .forEach(candidate -> {
                    double distance = haversine(lat, lon, candidate.getStopLat(), candidate.getStopLon());
                    if (distance < request.getRadius() * 1000) {
                        candidate.setDistance(distance);
                        foundStops.add(candidate);
                    }
                });

        if (!foundStops.isEmpty() && request.getLimit() > 1) {
            ArrayList<PhotonResult> resultsWithStops = new ArrayList<>(foundStops);
            resultsWithStops.addAll(results);
            return resultsWithStops;
        } else if (!foundStops.isEmpty()) {
            PhotonResult result = foundStops.iterator().next();
            return List.of(result);
        }
        return results;
    }

    private static void searchByLatLon(double lon, double lat, Set<PhotonResult> foundStops) {
        String hash = GeoHash.encodeHash(lat, lon, PRECISION_HASH_LENGTH);
        List<String> neighbors = GeoHash.neighbours(hash);
        Set<String> area = new HashSet<>(neighbors);
        area.add(hash);

        area.stream().map(nearbyHash -> gtfsStops.getIndexes().getOrDefault(nearbyHash, List.of()))
                .flatMap(Collection::stream)
                .forEach(candidate -> {
                    double distance = haversine(lat, lon, candidate.getStopLat(), candidate.getStopLon());
                    if (distance < 250) {
                        foundStops.add(candidate);
                    }
                });
    }

    private static Set<PhotonResult> searchNearbyCoordinatesOfResults(List<PhotonResult> results, PhotonRequest request) {
        Set<PhotonResult> foundStops = new HashSet<>();

        if (request.getLocationForBias() == null) results.forEach(
                result -> searchByLatLon(
                        result.getCoordinates()[0],
                        result.getCoordinates()[1],
                        foundStops));

        else searchByLatLon(request.getLocationForBias().getX(),
                request.getLocationForBias().getY(),
                foundStops);

        return foundStops;
    }

    private static Set<PhotonResult> searchByName(PhotonRequest request) {
        String[] query = request.getQuery().toLowerCase().split(" ");
        for (String name : query) {
            if (gtfsStops.getInGroups().get(name) == null) continue;
            Set<PhotonResult> results = new HashSet<>(gtfsStops.getInGroups().get(name));
            if (!results.isEmpty())
                return results;
        }
        for (String name : query) {
            Set<PhotonResult> results = gtfsStops.getStops().stream()
                    .filter(stop -> stop.getFullStopName()
                            .matches("(?i).*" + name + ".*"))
                    .collect(Collectors.toUnmodifiableSet());
            if (!results.isEmpty())
                return results;
        }
        return Set.of();
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
