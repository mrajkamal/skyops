package com.indusair.skyops.flightinfo.model;

import java.time.Instant;

/**
 * Aggregated statistics returned to the dashboard stat bar.
 * Computed from the cached flight list on every request — no extra DB call.
 */
public record FlightStats(
    int total,
    int active,
    int delayed,
    int onGround,
    long dataAgeSeconds,
    Instant computedAt
) {}
