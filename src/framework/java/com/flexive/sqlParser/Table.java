/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.sqlParser;

import com.flexive.shared.search.query.VersionFilter;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Table
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Table {

    public enum TYPE { CONTENT }

    private static final Pattern PAT_LANGUAGE_VARIANT = Pattern.compile("[e|d|z|p][0-9]"); // workaround for additional languages (e1, d1, ...)

    private String sAlias;
    private TYPE tType;
    private HashMap<Filter.TYPE,Filter> filters;
    private String searchLanguages[];

    /**
     * Returns the languages that should be searched in.
     *
     * @return the languages that should be searched in
     */
    public String[] getSearchLanguages() {
        return searchLanguages==null?new String[0]:searchLanguages;
    }

    /**
     * Returns the version filter for the table.
     * <p />
     * Possible return values are:<br>
     * "MAX","LIVE","ALL" or an integer array (eg "1,4,7")
     *
     * @return the version filter for the table.
     */
    public String getVersionFilter() {
        Filter f = getFilter(Filter.TYPE.VERSION);
        if (f==null) {
            return VersionFilter.MAX.toString();
        } else {
            return f.getValue();
        }
    }

    /**
     * Sets the available languages for the table.
     * <p />
     *
     * @param isoCodes the iso codes
     * @throws SqlParserException
     */
    protected void setSearchLanguages(String isoCodes[]) throws SqlParserException {
        // searchLanguages contains all available options, filter out all unused
        // entries.
        if (isoCodes==null) {
            isoCodes = new String[0];
        }
        for (String code:isoCodes) {
            boolean found = false;
            for (String loc:Locale.getISOLanguages()) {
                if (code.equalsIgnoreCase(loc)) {
                    found =true;
                    break;
                }
            }

            if (!found && !PAT_LANGUAGE_VARIANT.matcher(code).matches()) {
                throw new SqlParserException("ex.sqlSearch.languages.unknownIsoCode",code);
            }
        }
        this.searchLanguages = isoCodes;
    }

    /**
     * Constructor.
     *
     * @param type the type
     * @param alias the alias
     * @throws SqlParserException
     */
    protected Table(String type,String alias) throws SqlParserException {
        if (!type.equalsIgnoreCase("CONTENT")) {
            throw new SqlParserException("ex.sqlSearch.table.typeNotSupported",type.toUpperCase());
        }
        //this.searchLanguages = getAvailableLanguages();
        this.tType = TYPE.CONTENT;
        this.sAlias = alias.toUpperCase();
        this.filters = new HashMap<Filter.TYPE,Filter>(5);
    }

    /**
     * Adds a table specific filter.
     *
     * @param f the filter to add
     */
    protected void addFilter(Filter f) {
        this.filters.put(f.getType(),f);
    }

    /**
     * Returns the table alias.
     *
     * @return the table alias.
     */
    public String getAlias() {
        return sAlias;
    }

    /**
     * Returns the table type.
     *
     * @return the table type
     */
    public TYPE getType() {
        return tType;
    }

    /**
     * Returns a string representation of the table.
     *
     * @return a string representation of the table.
     */
    public String toString() {
        return "table[alias:"+sAlias+";type:"+tType+"]";
    }

    /**
     * Returns the desired filter, or null if the filter was not specified and has no
     * default value.
     *
     * @param t the filter to get
     * @return the filter
     */
    public Filter getFilter(Filter.TYPE t) {
        return this.filters.get(t);
    }

}
