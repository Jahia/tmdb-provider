package org.jahia.modules.tmdbprovider;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * Keeps the answers of TMDB. Values are JSON strings, so an entry never shares mutable state.
 */
public interface TMDBCache {

    /** A cache that keeps nothing, for when the Jahia cache cannot be created. */
    TMDBCache NONE = new TMDBCache() {
        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public void put(String key, String value) {
            // keeps nothing
        }

        @Override
        public void clear() {
            // nothing to clear
        }
    };

    String get(String key);

    void put(String key, String value);

    void clear();

    /** The Jahia ehcache, with a time to live on every entry so that changes on TMDB show up. */
    static TMDBCache of(Cache cache, int timeToLiveSeconds) {
        return new TMDBCache() {
            @Override
            public String get(String key) {
                Element element = cache.get(key);
                return element != null && element.getObjectValue() instanceof String ? (String) element.getObjectValue() : null;
            }

            @Override
            public void put(String key, String value) {
                Element element = new Element(key, value);
                if (timeToLiveSeconds > 0) {
                    element.setTimeToLive(timeToLiveSeconds);
                }
                cache.put(element);
            }

            @Override
            public void clear() {
                cache.removeAll();
            }
        };
    }
}
