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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxSQLSearchParams;
import com.flexive.shared.search.ResultLocation;
import com.flexive.shared.search.ResultViewType;
import com.flexive.shared.search.query.QueryRootNode;

import javax.ejb.Remote;
import java.util.Collection;

@Remote
public interface SearchEngine {
    /**
     * The wildcard character selects all properties of the result content type.
     */
    String PROP_WILDCARD = "*";
    /**
     * The user wildcard selects the properties predefined by the user through the
     * {@link com.flexive.shared.interfaces.ResultPreferencesEngine}.
     *
     */
    String PROP_USERWILDCARD = "@*";
    /**
     * A custom query property.
     * @since 3.2.1
     * @see com.flexive.shared.search.FxSQLSearchParams#addCustomSqlQuery(String, String)
     */
    String PROP_CUSTOM_SQL = "@CUSTOM_SQL";
    /**
     * The default query timeout in seconds.
     * @since 3.1
     */
    int DEFAULT_QUERY_TIMEOUT = 30;
    /**
     * The default size of a search query result.
     * @since 3.1
     */
    int DEFAULT_MAX_ROWS = 2000;

    /**
     * Executes a query.
     *
     * @param query the query to execute
     * @return the result set
     * @throws FxApplicationException if the search failed
     */
    FxResultSet search(String query) throws FxApplicationException;

    /**
     * Executes a query.
     *
     * @param query the query to execute
     * @param startIndex return data starting at the given row, 0 based
     * @param fetchRows the maximum rows to fetch, -1 to fetch all rows
     * @param params all additional search options
     * @return the result set
     * @throws FxApplicationException if the search failed
     */
    FxResultSet search(String query, int startIndex, int fetchRows, FxSQLSearchParams params)
            throws FxApplicationException;

    /**
     * Executes a query.
     *
     * @param query the query to execute
     * @param startIndex return data starting at the given row, 0 based
     * @param fetchRows the maximum rows to fetch, -1 to fetch all rows.
     * @param params all additional search options
     * @param location  the result location
     * @param viewType  the result view type
     * @return the result set
     * @throws FxApplicationException if the search failed
     */
    FxResultSet search(String query, int startIndex, int fetchRows, FxSQLSearchParams params,
                              ResultLocation location, ResultViewType viewType)
            throws FxApplicationException;

    /**
     * Returns the last time that any content that affects queries was changed.
     *
     * @param live if true only the last changes of live contents are checked
     * @return the last time that any content that affects queries was changed
     */
    long getLastContentChange(boolean live);

    /**
     * Store the given query for the current user.
     *
     * @param query the query to be stored
     * @throws FxApplicationException TODO
     */
    void save(QueryRootNode query) throws FxApplicationException;

    /**
     * Sets the user-defined default query for the query's type and location.
     *
     * @param query the query to be stored
     * @throws FxApplicationException   if the default query could not be set.
     * @see com.flexive.shared.search.query.QueryRootNode#getType()
     * @see com.flexive.shared.search.query.QueryRootNode#getLocation()
     */
    void saveDefault(QueryRootNode query) throws FxApplicationException;

    /**
     * Sets the system-wide default query for the query's type and location.
     * Only the global supervisor may set the default query.
     *
     * @param query the default query to be set
     * @throws FxApplicationException   if the default query could not be set, or
     * the caller lacks supervisor privileges
     * @see com.flexive.shared.search.query.QueryRootNode#getType()
     * @see com.flexive.shared.search.query.QueryRootNode#getLocation()
     */
    void saveSystemDefault(QueryRootNode query) throws FxApplicationException;

    /**
     * Load the query of the given name, for the current user.
     *
     * @param location the query location
     * @param name     query to be loaded @return  the loaded query tree
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxNotFoundException    if the query does not exist
     * @return  the query root node
     */
    QueryRootNode load(ResultLocation location, String name) throws FxApplicationException;

    /**
     * Load the default query for the given type/location. If none is set, an empty query is returned.
     *
     * @param location  the query location, usually matched to the location of its result
     * @return  the default query for the given type/location
     * @throws FxApplicationException   if the default query could not be loaded
     */
    QueryRootNode loadDefault(ResultLocation location) throws FxApplicationException;

    /**
     * Load the system-wide default query for the given type/location. If none is set, an empty query is returned.
     *
     * @param location  the query location, usually matched to the location of its result
     * @return  the default query for the given type/location
     * @throws FxApplicationException   if the default query could not be loaded
     */
    QueryRootNode loadSystemDefault(ResultLocation location) throws FxApplicationException;

    /**
     * Returns the names of the stored queries for the calling user/location. The query names
     * are also the keys for retrieving the actual search query via
     * {@link #load(com.flexive.shared.search.ResultLocation, String)}.
     *
     * @param location  the query location, usually matched to the location of its result
     * @return  all stored queries for the given location of the current user
     * @throws FxApplicationException   if the query names could not be retrieved
     */
    Collection<String> loadNames(ResultLocation location) throws FxApplicationException;

    /**
     * Remove the query of the given name and type from the database.
     *
     * @param location the query location
     * @param name     the query to be removed @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxApplicationException if the query could not be deleted
     */
    void remove(ResultLocation location, String name) throws FxApplicationException;
}
