package com.indusair.skyops.flightinfo.client;

import com.indusair.skyops.flightinfo.model.FlightState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the OpenSky Network REST API.
 * https://opensky-network.org/api/states/all
 *
 * Bounding box for India: lamin=8, lamax=37, lomin=68, lomax=97
 * No API key required for basic access (anonymous rate limit: 10 req/min).
 */
@Component
public class OpenSkyClient {

    private static final Logger log = LoggerFactory.getLogger(OpenSkyClient.class);

    private final RestTemplate restTemplate;
    private final String openSkyUrl;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer   latencyTimer;

    public OpenSkyClient(
            RestTemplate restTemplate,
            @Value("${opensky.url:https://opensky-network.org/api}") String openSkyUrl,
            MeterRegistry meterRegistry) {
        this.restTemplate   = restTemplate;
        this.openSkyUrl     = openSkyUrl;
        this.successCounter = Counter.builder("opensky.api.calls")
            .tag("result", "success").register(meterRegistry);
        this.errorCounter   = Counter.builder("opensky.api.calls")
            .tag("result", "error").register(meterRegistry);
        this.latencyTimer   = Timer.builder("opensky.api.latency")
            .description("OpenSky API call latency").register(meterRegistry);
    }

    /**
     * Fetch all flights currently over India.
     * Returns an empty list if the API is unavailable — never throws.
     */
    @SuppressWarnings("unchecked")
    public List<FlightState> getFlightsOverIndia() {
        return latencyTimer.record(() -> {
            try {
                String url = openSkyUrl + "/states/all?lamin=8&lamax=37&lomin=68&lomax=97";
                log.debug("Calling OpenSky API: {}", url);

                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response == null || !response.containsKey("states")) {
                    log.warn("OpenSky returned null or empty response");
                    successCounter.increment();
                    return List.of();
                }

                List<Object[]> rawStates = (List<Object[]>) response.get("states");
                List<FlightState> flights = new ArrayList<>(rawStates.size());

                for (Object raw : rawStates) {
                    try {
                        Object[] arr = ((List<?>) raw).toArray();
                        flights.add(FlightState.fromArray(arr));
                    } catch (Exception e) {
                        log.debug("Skipping malformed flight state: {}", e.getMessage());
                    }
                }

                log.info("OpenSky returned {} flights over India", flights.size());
                successCounter.increment();
                return flights;

            } catch (Exception e) {
                log.error("OpenSky API call failed: {}", e.getMessage());
                errorCounter.increment();
                return List.of();
            }
        });
    }
}
