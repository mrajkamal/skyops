package com.indusair.skyops.flightinfo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;

/**
 * Represents a single aircraft state vector from the OpenSky Network API.
 * https://opensky-network.org/apidoc/rest.html#response
 *
 * OpenSky returns a JSON array per state. The index positions are mapped here.
 * Index: 0=icao24, 1=callsign, 2=origin_country, 3=time_position, 4=last_contact,
 *        5=longitude, 6=latitude, 7=baro_altitude, 8=on_ground, 9=velocity,
 *        10=true_track, 11=vertical_rate, 12=sensors, 13=geo_altitude,
 *        14=squawk, 15=spi, 16=position_source
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FlightState(
    String icao24,
    String callsign,
    String originCountry,
    Long timePosition,
    Long lastContact,
    Double longitude,
    Double latitude,
    Double baroAltitude,
    Boolean onGround,
    Double velocity,
    Double trueTrack,
    Double verticalRate,
    Double geoAltitude,
    String squawk,
    Instant fetchedAt
) implements Serializable {

    /** Build from the raw OpenSky array response */
    public static FlightState fromArray(Object[] arr) {
        return new FlightState(
            getString(arr, 0),
            getString(arr, 1) != null ? getString(arr, 1).trim() : null,
            getString(arr, 2),
            getLong(arr, 3),
            getLong(arr, 4),
            getDouble(arr, 5),
            getDouble(arr, 6),
            getDouble(arr, 7),
            getBoolean(arr, 8),
            getDouble(arr, 9),
            getDouble(arr, 10),
            getDouble(arr, 11),
            getDouble(arr, 13),
            getString(arr, 14),
            Instant.now()
        );
    }

    private static String  getString(Object[] arr, int i)  { return i < arr.length && arr[i] != null ? arr[i].toString() : null; }
    private static Long    getLong(Object[] arr, int i)    { return i < arr.length && arr[i] != null ? ((Number) arr[i]).longValue() : null; }
    private static Double  getDouble(Object[] arr, int i)  { return i < arr.length && arr[i] != null ? ((Number) arr[i]).doubleValue() : null; }
    private static Boolean getBoolean(Object[] arr, int i) { return i < arr.length && arr[i] != null ? (Boolean) arr[i] : null; }
}
