package org.jahia.modules.tmdbprovider;

import org.jahia.modules.external.ExternalData;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TMDBDataSourceTest {

    private static final String IMG = "https://image.tmdb.org/t/p/";

    /** Answers from src/test/resources/fixtures: /3/tv/1/season/2 with language=en is tv_1_season_2@en.json. */
    static class FixtureApi implements TMDBApi {
        final List<String> requests = new ArrayList<>();

        @Override
        public JSONObject get(String path, String... params) throws RepositoryException {
            String language = null;
            for (int i = 0; i + 1 < params.length; i += 2) {
                if ("language".equals(params[i])) {
                    language = params[i + 1];
                }
            }
            String name = path.substring("/3/".length()).replace('/', '_') + (language != null ? "@" + language : "");
            requests.add(name);
            try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name + ".json")) {
                if (in == null) {
                    throw new RepositoryException("No fixture " + name);
                }
                return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }
    }

    static class MapCache implements TMDBCache {
        final Map<String, String> map = new HashMap<>();

        @Override
        public String get(String key) {
            return map.get(key);
        }

        @Override
        public void put(String key, String value) {
            map.put(key, value);
        }

        @Override
        public void clear() {
            map.clear();
        }
    }

    private TMDBDataSource source;
    private FixtureApi api;

    @BeforeEach
    void setUp() {
        source = new TMDBDataSource();
        api = new FixtureApi();
        source.applySettings("en, fr, es", "84773, abc, 84773", "119", "123,1361", "w342", "w1280", "w185", "w300");
        source.use(api, new MapCache(), false);
    }

    @Test
    void rootListsEveryFolder() throws RepositoryException {
        assertEquals(List.of("movies", "series", "collections", "featured", "persons", "lists"), source.getChildren("/"));
    }

    @Test
    void configuredFoldersKeepNumericIdsOnce() throws RepositoryException {
        assertEquals(List.of("84773"), source.getChildren("/series"));
        assertEquals(List.of("119"), source.getChildren("/collections"));
        assertEquals(List.of("123", "1361"), source.getChildren("/featured"));
    }

    @Test
    void seriesCarriesItsProperties() throws RepositoryException {
        ExternalData tv = source.getItemByIdentifier("tv-84773");
        assertEquals("/series/84773", tv.getPath());
        assertEquals("jnt:tvSeries", tv.getType());
        Map<String, String[]> p = tv.getProperties();
        assertArrayEquals(new String[]{"2"}, p.get("number_of_seasons"));
        assertArrayEquals(new String[]{"tt0000001"}, p.get("imdb_id"));
        assertArrayEquals(new String[]{"person-1276960"}, p.get("created_by"));
        assertArrayEquals(new String[]{"Network A"}, p.get("networks"));
        assertArrayEquals(new String[]{"Scripted"}, p.get("series_type"));
        assertArrayEquals(new String[]{"elves"}, p.get("j:keywords"));
        assertArrayEquals(new String[]{IMG + "w1280/backdrop.jpg"}, p.get("backdrop_path"));
        assertArrayEquals(new String[]{"2022-09-01T00:00:00.000+00:00"}, p.get("first_air_date"));
        assertNull(p.get("homepage"), "an empty TMDB value is no property");
        assertEquals(Set.of("en", "fr", "es"), tv.getLazyI18nProperties().keySet());
    }

    @Test
    void seriesListsSeasonsAndCredits() throws RepositoryException {
        assertEquals(List.of("season_0", "season_1", "cast_1", "crew_Writer_2", "crew_Executive Producer_2"),
                source.getChildren("/series/84773"));
    }

    @Test
    void seasonAndEpisodeResolveFromTheirPaths() throws RepositoryException {
        ExternalData season = source.getItemByPath("/series/84773/season_1");
        assertEquals("tvseason-84773-1", season.getId());
        assertEquals("jnt:tvSeason", season.getType());
        assertArrayEquals(new String[]{"2"}, season.getProperties().get("episode_count"));
        assertEquals(List.of("episode_1", "episode_2"), source.getChildren("/series/84773/season_1"));

        ExternalData episode = source.getItemByPath("/series/84773/season_1/episode_1");
        assertEquals("tvepisode-84773-1-1", episode.getId());
        assertEquals("jnt:tvEpisode", episode.getType());
        assertArrayEquals(new String[]{"66"}, episode.getProperties().get("runtime"));
        assertArrayEquals(new String[]{IMG + "w300/still1.jpg"}, episode.getProperties().get("still_path"));
        assertNull(source.getItemByPath("/series/84773/season_1/episode_2").getProperties().get("still_path"));
    }

    @Test
    void seriesCreditsComeFromAggregateCredits() throws RepositoryException {
        ExternalData cast = source.getItemByPath("/series/84773/cast_1");
        assertEquals("jnt:cast", cast.getType());
        assertArrayEquals(new String[]{"Character A"}, cast.getProperties().get("character"));
        assertArrayEquals(new String[]{"16"}, cast.getProperties().get("episode_count"));
        assertArrayEquals(new String[]{"person-1"}, cast.getProperties().get("person"));
        assertArrayEquals(new String[]{IMG + "w185/actor.jpg"}, cast.getProperties().get("profile"));

        ExternalData writer = source.getItemByPath("/series/84773/crew_Writer_2");
        assertEquals("jnt:crew", writer.getType());
        assertArrayEquals(new String[]{"3"}, writer.getProperties().get("episode_count"));
        assertArrayEquals(new String[]{"Writing"}, writer.getProperties().get("department"));
        ExternalData producer = source.getItemByPath("/series/84773/crew_Executive Producer_2");
        assertArrayEquals(new String[]{"16"}, producer.getProperties().get("episode_count"));
        assertThrows(PathNotFoundException.class, () -> source.getItemByPath("/series/84773/crew_Director_2"));
    }

    @Test
    void translatedValuesFallBackToEnglish() throws RepositoryException {
        assertArrayEquals(new String[]{"Serie Uno"}, source.getI18nPropertyValues("/series/84773", "es", "jcr:title"));
        assertArrayEquals(new String[]{"An English overview."}, source.getI18nPropertyValues("/series/84773", "es", "overview"));
        assertArrayEquals(new String[]{IMG + "w342/poster-es.jpg"}, source.getI18nPropertyValues("/series/84773", "es", "poster_path"));
        assertArrayEquals(new String[0], source.getI18nPropertyValues("/series/84773", "en", "tagline"));

        assertArrayEquals(new String[]{"Temporada 1"}, source.getI18nPropertyValues("/series/84773/season_1", "es", "jcr:title"));
        assertArrayEquals(new String[]{"Episodio uno"}, source.getI18nPropertyValues("/series/84773/season_1/episode_1", "es", "jcr:title"));
        assertArrayEquals(new String[]{"The first episode."}, source.getI18nPropertyValues("/series/84773/season_1/episode_1", "es", "overview"));
    }

    @Test
    void collectionPartsAreInReleaseOrder() throws RepositoryException {
        ExternalData collection = source.getItemByPath("/collections/119");
        assertEquals("collection-119", collection.getId());
        assertEquals("jnt:movieCollection", collection.getType());
        assertArrayEquals(new String[]{"movie-120", "movie-121", "movie-122", "movie-999"}, collection.getProperties().get("parts"));
        assertArrayEquals(new String[]{"Collection A (fr)"}, source.getI18nPropertyValues("/collections/119", "fr", "jcr:title"));
        assertArrayEquals(new String[]{IMG + "w342/coll-fr.jpg"}, source.getI18nPropertyValues("/collections/119", "fr", "poster_path"));
    }

    @Test
    void movieKeepsItsIdentifiersAndLinksItsCollection() throws RepositoryException {
        ExternalData movie = source.getItemByIdentifier("movie-120");
        assertEquals("/movies/2001/12/120", movie.getPath());
        assertEquals("jnt:movie", movie.getType());
        Map<String, String[]> p = movie.getProperties();
        assertArrayEquals(new String[]{"collection-119"}, p.get("collection"));
        assertArrayEquals(new String[]{"elf"}, p.get("j:keywords"));
        assertArrayEquals(new String[]{"Adventure"}, p.get("j:tagList"));
        assertArrayEquals(new String[]{IMG + "w1280/movie-back.jpg"}, p.get("backdrop_path"));
        assertTrue(movie.getLazyProperties().contains("runtime"));
        assertArrayEquals(new String[]{"179"}, source.getPropertyValues("/movies/2001/12/120", "runtime"));
        assertArrayEquals(new String[0], source.getPropertyValues("/movies/2001/12/120", "homepage"));
        assertArrayEquals(new String[]{"Movie One"}, source.getI18nPropertyValues("/movies/2001/12/120", "en", "jcr:title"));
    }

    @Test
    void movieCreditsKeepTheirNames() throws RepositoryException {
        assertEquals(List.of("cast_1_10", "crew_Director_20"), source.getChildren("/movies/2001/12/120"));
        ExternalData cast = source.getItemByPath("/movies/2001/12/120/cast_1_10");
        assertEquals("moviecredits-120-cast_1_10", cast.getId());
        assertArrayEquals(new String[]{"Character C"}, cast.getProperties().get("character"));
        ExternalData director = source.getItemByPath("/movies/2001/12/120/crew_Director_20");
        assertArrayEquals(new String[]{"Director"}, director.getProperties().get("job"));
        assertArrayEquals(new String[]{"person-20"}, director.getProperties().get("person"));
    }

    @Test
    void featuredMoviesAreReferences() throws RepositoryException {
        ExternalData ref = source.getItemByPath("/featured/123");
        assertEquals("featuredref-123", ref.getId());
        assertEquals("jnt:contentReference", ref.getType());
        assertArrayEquals(new String[]{"movie-123"}, ref.getProperties().get("j:node"));
    }

    @Test
    void technicalChildrenAreNotFound() {
        assertThrows(PathNotFoundException.class, () -> source.getItemByPath("/series/84773/j:translation_fr"));
        assertThrows(PathNotFoundException.class, () -> source.getItemByPath("/movies/2001/12/120/j:acl"));
        assertThrows(PathNotFoundException.class, () -> source.getItemByPath("/unknown/1"));
    }

    @Test
    void malformedIdentifiersAreNotFound() {
        for (String id : Arrays.asList("tv-abc", "tvseason-1", "tvepisode-1-2", "tvseason-1-x", "collection-", "featuredref-x", "nothing")) {
            assertThrows(ItemNotFoundException.class, () -> source.getItemByIdentifier(id), id);
        }
    }

    @Test
    void answersAreCached() throws RepositoryException {
        source.getItemByIdentifier("tv-84773");
        int calls = api.requests.size();
        source.getItemByIdentifier("tv-84773");
        source.getI18nPropertyValues("/series/84773", "en", "jcr:title");
        assertEquals(calls, api.requests.size());
    }

    @Test
    void pathsAndIdentifiersMatch() {
        assertEquals("tvcredits-84773-cast_1", TMDBDataSource.identifierForPath("/series/84773/cast_1".split("/")));
        assertEquals("movies-folder-2001/12", TMDBDataSource.identifierForPath("/movies/2001/12".split("/")));
        assertEquals("series-rootfolder", TMDBDataSource.identifierForPath("/series".split("/")));
        assertEquals("movieref-abc-120", TMDBDataSource.identifierForPath("/lists/abc/120".split("/")));
    }
}
