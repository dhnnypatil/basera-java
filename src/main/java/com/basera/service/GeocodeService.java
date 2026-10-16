package com.basera.service;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a place name ("Calangute, Goa, India") into map coordinates (utils/geocode.js).
 *
 * Uses OpenStreetMap's free Nominatim service - no API key needed. If the lookup fails or times out we return
 * null and the listing is saved without a map, instead of the whole request failing.
 *
 * RestClient is Spring's HTTP client - the job `fetch()` did in Node.
 */
@Service
public class GeocodeService {

    private static final Logger log = LoggerFactory.getLogger(GeocodeService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final RestClient http;
    private final JsonMapper jsonMapper;
    private final boolean enabled;

    public GeocodeService(JsonMapper jsonMapper, @Value("${basera.geocoding.enabled:true}") boolean enabled) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        requestFactory.setReadTimeout(TIMEOUT); // AbortSignal.timeout(5000)
        this.http = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Basera-College-Project/2.0") // required by Nominatim's usage policy
                .build();
        this.jsonMapper = jsonMapper;
        this.enabled = enabled;
    }

    /** Returns the point for `placeName`, or null if it can't be found. */
    public GeoJsonPoint geocode(String placeName) {
        if (!enabled) {
            return null;
        }
        try {
            String body = http.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .queryParam("q", placeName)
                            .build())
                    .retrieve()
                    .body(String.class);
            JsonNode results = jsonMapper.readTree(body);
            if (!results.isArray() || results.isEmpty()) {
                return null;
            }
            JsonNode first = results.get(0);
            // GeoJsonPoint(x, y) = (longitude, latitude), stored as coordinates: [lng, lat] like the Node app.
            return new GeoJsonPoint(first.get("lon").asDouble(), first.get("lat").asDouble());
        } catch (Exception e) {
            log.warn("Geocoding failed for \"{}\": {}", placeName, e.getMessage());
            return null;
        }
    }
}
