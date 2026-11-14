package org.jahia.modules.tmdbprovider;

import org.json.JSONObject;

import javax.jcr.RepositoryException;

/**
 * Reads one resource of the TMDB API. The data source depends on this interface, so tests answer it
 * from fixtures.
 */
public interface TMDBApi {

    /**
     * @param path   the API path, for example {@code /3/tv/84773}
     * @param params query parameters as name, value pairs
     * @return the JSON body of the answer
     * @throws RepositoryException when TMDB cannot be reached or answers an error
     */
    JSONObject get(String path, String... params) throws RepositoryException;
}
