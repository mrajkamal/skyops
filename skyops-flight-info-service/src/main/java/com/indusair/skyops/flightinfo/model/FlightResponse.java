package com.indusair.skyops.flightinfo.model;

import java.time.Instant;
import java.util.List;

/**
 * Wrapper returned to callers of /api/flights.
 * Includes cache metadata so the UI can show staleness.
 */
public record FlightResponse(
    int count,
    List<FlightState> flights,
    Instant timestamp,
    String source       // "live" or "cache"
) {
    public static FlightResponse of(List<FlightState> flights, String source) {
        return new FlightResponse(flights.size(), flights, Instant.now(), source);
    }
}
