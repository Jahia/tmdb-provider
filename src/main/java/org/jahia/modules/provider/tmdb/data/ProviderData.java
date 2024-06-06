/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.provider.tmdb.data;

import org.jahia.modules.external.ExternalData;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Jerome Blanchard
 */
public class ProviderData implements Serializable {

    private String id;
    private String type;
    private String name;
    private Map<String,String[]> properties;

    public ProviderData() {
    }

    public ProviderData(String id, String type, String name, Map<String, String[]> properties) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public ProviderData withId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public ProviderData withType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String[]> getProperties() {
        return properties;
    }

    public ProviderData withProperties(Map<String, String[]> properties) {
        this.properties = properties;
        return this;
    }

    public ExternalData toExternalData(String path) {
        return new ExternalData(id, path, type, properties);
    }

    public ExternalData toExternalData(String path, Set<String> lazyProps, Set<String> lazyI18nProps) {
        ExternalData data = new ExternalData(id, path, type, properties);
        data.setLazyProperties(new HashSet<>(lazyProps));
        data.setLazyI18nProperties(Map.of("en", new HashSet<>(lazyI18nProps), "fr", new HashSet<>(lazyI18nProps)));
        return data;
    }
}
