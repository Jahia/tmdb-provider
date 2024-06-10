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

import com.google.common.collect.Streams;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.provider.tmdb.helper.Naming;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A list of 50 predefined years from current year
 *
 * @author Jerome Blanchard
 */
@Component(service = { YearsCollection.class, ProviderDataCollection.class}, scope = ServiceScope.SINGLETON, immediate = true)
public class YearsCollection implements ProviderDataCollection {

    public static final String ID_PREFIX = "year-";

    private static final List<ProviderData> YEARS = new ArrayList<>();
    static {
        YEARS.addAll(IntStream.rangeClosed(Calendar.getInstance().get(Calendar.YEAR) - 5, Calendar.getInstance().get(Calendar.YEAR))
                .mapToObj(Integer::toString).map(YearsCollection::map).collect(Collectors.toList()));
        YEARS.add(YearsCollection.map("0000"));
    }

    @Override
    public ProviderData getData(String identifier) {
        return YEARS.stream().filter(y -> identifier.equals(y.getId())).findFirst().orElse(null);
    }

    public List<ProviderData> list() {
        return YEARS;
    }

    protected static ProviderData map(String year) {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(Constants.JCR_TITLE, new String[] { year });
        return new ProviderData(ID_PREFIX + year, Naming.NodeType.CONTENT_FOLDER, year, properties);
    }

}
