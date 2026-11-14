# tmdb-provider

Mounts [The Movie Database (TMDB)](https://www.themoviedb.org) in the JCR of Jahia as an external
data provider: movies, TV series with their seasons and episodes, collections, people, and the
lists of a TMDB account.

This product uses the TMDB API but is not endorsed or certified by TMDB.

## Prerequisites

* Jahia 8.2.1.0 or later
* the modules `external-provider`, `bootstrap3-core` and `bootstrap3-components` (the JSP views
  use the last two)

## Setup

1. Request an API read access token from TMDB (Settings > API).
2. Deploy the module.
3. Configure the PID `org.jahia.modules.tmdbprovider` (Jahia Tools > OSGi configuration > TMDB
   Provider, or a provisioning `editConfiguration`):

| Key | Default | What it does |
|---|---|---|
| `apiReadAccessToken` | empty | The v4 read access token, sent as a Bearer header. When empty, the environment variable `TMDB_API_READ_ACCESS_TOKEN` is used. |
| `apiKey` | empty | The v3 API key, used only when there is no token. When empty, `TMDB_API_KEY` is used. |
| `mountPoint` | `/sites/digitall/contents/tmdb` | Where the database is mounted. Mount it under the site that renders it. |
| `languages` | `en,fr` | The languages whose titles, overviews and posters are read from TMDB. A missing translation falls back to English. |
| `seriesIds` | empty | TMDB ids of the series listed under `/series`. |
| `collectionIds` | empty | TMDB ids of the collections listed under `/collections`. |
| `featuredMovieIds` | empty | TMDB ids of the movies listed under `/featured`. |
| `cacheTimeToLive` | `21600` | Seconds an answer of TMDB stays in the cache. |
| `posterSize`, `backdropSize`, `profileSize`, `stillSize` | `w342`, `w1280`, `w185`, `w300` | Image sizes, from the `images` section of the TMDB configuration. |

The provider does not start without a token or a key.

## What the mount holds

| Path | Type |
|---|---|
| `movies/YYYY/MM/<id>` | `jnt:movie`, with `cast_<castId>_<person>` and `crew_<job>_<person>` children |
| `series/<id>` | `jnt:tvSeries`, with `season_<n>`, `cast_<person>` and `crew_<job>_<person>` children |
| `series/<id>/season_<n>/episode_<m>` | `jnt:tvSeason`, then `jnt:tvEpisode` |
| `collections/<id>` | `jnt:movieCollection`; `parts` references its movies in release order |
| `featured/<id>` | `jnt:contentReference` to a movie |
| `persons/<id>` | `jnt:moviePerson` |
| `lists/<id>` | `jnt:moviesList` of the TMDB account (needs a session opened with the `tmdbtoken` action) |

A movie references its collection with `collection`, a series its creators with `created_by`, and a
credit its person with `person`.

## Build

```bash
mvn clean install
```

The build needs JDK 17 and Maven 3.8. The unit tests answer from fixtures under
`src/test/resources/fixtures` and never call TMDB.
