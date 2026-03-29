package com.indusair.skyops.flightinfo.controller;

import com.indusair.skyops.flightinfo.model.FlightResponse;
import com.indusair.skyops.flightinfo.model.FlightStats;
import com.indusair.skyops.flightinfo.service.FlightInfoService;
import io.micrometer.core.annotation.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for flight information.
 *
 * Endpoints:
 *   GET /api/flights           — alias for /active (Angular-compatible)
 *   GET /api/flights/active    — all flights over India (cached)
 *   GET /api/flights/stats     — aggregated dashboard stats
 *   GET /api/flights/{callsign} — single flight by callsign
 */
@RestController
@RequestMapping("/api/flights")
public class FlightInfoController {

    private final FlightInfoService flightInfoService;

    public FlightInfoController(FlightInfoService flightInfoService) {
        this.flightInfoService = flightInfoService;
    }

    /**
     * All flights currently over India — primary endpoint for the Angular dashboard.
     * Served from Redis cache (Chapter 3). Cache miss triggers a live OpenSky call.
     */
    @GetMapping({"", "/active"})
    @Timed(value = "flights.active.request", description = "Time to serve active flights")
    public ResponseEntity<FlightResponse> getActiveFlights() {
        FlightResponse response = flightInfoService.getFlightsOverIndia();
        return ResponseEntity.ok()
            .header("X-Data-Source", response.source())
            .header("X-Flight-Count", String.valueOf(response.count()))
            .body(response);
    }

    /**
     * Aggregated stats for the Live Map stat bar.
     * Derived from cached flights — no extra API call.
     */
    @GetMapping("/stats")
    @Timed(value = "flights.stats.request", description = "Time to compute flight stats")
    public ResponseEntity<FlightStats> getStats() {
        return ResponseEntity.ok(flightInfoService.getStats());
    }

    /**
     * Single flight by ICAO callsign (e.g. "AI101").
     */
    @GetMapping("/{callsign}")
    @Timed(value = "flights.callsign.request", description = "Time to serve single flight")
    public ResponseEntity<FlightResponse> getFlightByCallsign(
            @PathVariable String callsign) {
        FlightResponse response = flightInfoService.getFlightByCallsign(callsign);
        if (response.count() == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header("X-Data-Source", response.source())
            .body(response);
    }
}
