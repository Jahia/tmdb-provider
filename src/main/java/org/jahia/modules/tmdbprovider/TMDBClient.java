package org.jahia.modules.tmdbprovider;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Calls the TMDB API over HTTPS. A v4 read access token goes in the {@code Authorization} header, so
 * no secret appears in a URL or an access log; the v3 API key is only a fallback.
 */
public class TMDBClient implements TMDBApi {

    private static final Logger logger = LoggerFactory.getLogger(TMDBClient.class);
    private static final String BASE_URL = "https://api.themoviedb.org";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    /** TMDB answers 429 when a client goes over its rate limit; one retry after a pause is enough. */
    private static final int TOO_MANY_REQUESTS = 429;
    private static final long RETRY_PAUSE_MS = 1000;

    private final HttpClient httpClient;
    private final String readAccessToken;
    private final String apiKey;

    public TMDBClient(String readAccessToken, String apiKey) {
        this.readAccessToken = readAccessToken;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public JSONObject get(String path, String... params) throws RepositoryException {
        URI uri = buildUri(path, params);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET();
        if (StringUtils.isNotEmpty(readAccessToken)) {
            request.header("Authorization", "Bearer " + readAccessToken);
        }
        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == TOO_MANY_REQUESTS) {
                Thread.sleep(RETRY_PAUSE_MS);
                response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            }
            if (response.statusCode() != 200) {
                throw new RepositoryException("TMDB answered " + response.statusCode() + " for " + path);
            }
            return new JSONObject(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositoryException("Interrupted while querying TMDB for " + path, e);
        } catch (IOException | JSONException e) {
            throw new RepositoryException("Error while querying TMDB for " + path, e);
        } finally {
            logger.debug("TMDB {} answered in {} ms", path, System.currentTimeMillis() - start);
        }
    }

    private URI buildUri(String path, String... params) {
        StringBuilder query = new StringBuilder();
        if (StringUtils.isEmpty(readAccessToken) && StringUtils.isNotEmpty(apiKey)) {
            appendParam(query, "api_key", apiKey);
        }
        for (int i = 0; i + 1 < params.length; i += 2) {
            appendParam(query, params[i], params[i + 1]);
        }
        return URI.create(BASE_URL + path + (query.length() > 0 ? "?" + query : ""));
    }

    private static void appendParam(StringBuilder query, String name, String value) {
        if (value == null) {
            return;
        }
        if (query.length() > 0) {
            query.append('&');
        }
        query.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
