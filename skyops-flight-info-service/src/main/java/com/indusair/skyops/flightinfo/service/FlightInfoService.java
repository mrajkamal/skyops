package com.indusair.skyops.flightinfo.service;

import com.indusair.skyops.flightinfo.client.OpenSkyClient;
import com.indusair.skyops.flightinfo.model.FlightResponse;
import com.indusair.skyops.flightinfo.model.FlightState;
import com.indusair.skyops.flightinfo.model.FlightStats;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FlightInfoService {

    private static final Logger log = LoggerFactory.getLogger(FlightInfoService.class);

    private final OpenSkyClient openSkyClient;
    private final AtomicInteger flightCount = new AtomicInteger(0);
    private final AtomicLong lastFetchEpochMs = new AtomicLong(Instant.now().toEpochMilli());

    public FlightInfoService(OpenSkyClient openSkyClient, MeterRegistry meterRegistry) {
        this.openSkyClient = openSkyClient;
        Gauge.builder("flights.count", flightCount, AtomicInteger::get)
            .description("Number of flights over India")
            .register(meterRegistry);
    }

    public FlightResponse getFlightsOverIndia() {
        log.info("Fetching flights from OpenSky API");
        List<FlightState> flights = openSkyClient.getFlightsOverIndia();
        flightCount.set(flights.size());
        lastFetchEpochMs.set(Instant.now().toEpochMilli());
        return FlightResponse.of(flights, "live");
    }

    public FlightResponse getFlightByCallsign(String callsign) {
        log.info("Looking up callsign: {}", callsign);
        List<FlightState> all = openSkyClient.getFlightsOverIndia();
        List<FlightState> matched = all.stream()
            .filter(f -> callsign.equalsIgnoreCase(f.callsign()))
            .toList();
        return FlightResponse.of(matched, "live");
    }

    public FlightStats getStats() {
        FlightResponse response = getFlightsOverIndia();
        List<FlightState> flights = response.flights();
        long dataAgeSeconds = (Instant.now().toEpochMilli() - lastFetchEpochMs.get()) / 1000;
        return new FlightStats(
            flights.size(),
            (int) flights.stream().filter(f -> Boolean.FALSE.equals(f.onGround()) && f.velocity() != null && f.velocity() >= 100).count(),
            (int) flights.stream().filter(f -> Boolean.FALSE.equals(f.onGround()) && f.velocity() != null && f.velocity() < 100).count(),
            (int) flights.stream().filter(f -> Boolean.TRUE.equals(f.onGround())).count(),
            dataAgeSeconds,
            Instant.now()
        );
    }
}
