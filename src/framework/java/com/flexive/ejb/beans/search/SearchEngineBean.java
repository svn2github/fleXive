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
package com.flexive.ejb.beans.search;

import com.flexive.core.Database;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.storage.StorageManager;
import com.flexive.ejb.beans.EJBUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterDataBean;
import com.flexive.shared.configuration.ParameterPathBean;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.*;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@javax.ejb.TransactionManagement(javax.ejb.TransactionManagementType.CONTAINER)
@Stateless(name = "SearchEngine", mappedName="SearchEngine")
public class SearchEngineBean implements SearchEngine, SearchEngineLocal {
    private static final Log LOG = LogFactory.getLog(SearchEngineBean.class);
    private static final String DEFAULT_QUERY_NAME = SearchEngineBean.class.getName() + ".DEFAULTQUERY";


    @Resource
    javax.ejb.SessionContext ctx;
    @EJB
    ResultPreferencesEngineLocal resultPreferences;
    @EJB
    private SequencerEngineLocal seq;
    @EJB
    private BriefcaseEngineLocal briefcase;
    @EJB
    private ConfigurationEngineLocal configuration;
    @EJB
    private DivisionConfigurationEngineLocal divisionConfiguration;
    @EJB
    private TreeEngineLocal treeEngine;

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxResultSet search(String query) throws FxApplicationException {
        return search(query, 0, Integer.MAX_VALUE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxResultSet search(String query, int startIndex, int fetchRows, FxSQLSearchParams params)
            throws FxApplicationException {
        return search(query, startIndex, fetchRows, params, AdminResultLocations.DEFAULT, ResultViewType.LIST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxResultSet search(String query, int startIndex, int fetchRows, FxSQLSearchParams params, ResultLocation location, ResultViewType viewType) throws FxApplicationException {
        try {
            if (params == null) {
                params = new FxSQLSearchParams();
            }
            return new SqlSearch(seq, briefcase, treeEngine, query, startIndex, fetchRows, 
                    params, resultPreferences, location, viewType).executeQuery();
        } catch (FxSqlSearchException exc) {
            EJBUtils.rollback(ctx);
            throw exc;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getLastContentChange(boolean live) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(StorageManager.getLastContentChangeStatement(live));
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            //noinspection ThrowableInstanceNeverThrown
            throw new FxLoadException(LOG, e, "ex.sqlSearch.lastContentChange", e).asRuntimeException();
        } finally {
            Database.closeObjects(this.getClass(), con, stmt);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode load(ResultLocation location, String name) throws FxApplicationException {
        return check(configuration.get(getConfigurationParameter(location), name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode loadDefault(ResultLocation location) throws FxApplicationException {
        try {
            return load(location, DEFAULT_QUERY_NAME);
        } catch (FxNotFoundException e) {
            return new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location);
        } catch (FxApplicationException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public QueryRootNode loadSystemDefault(ResultLocation location) throws FxApplicationException {
        try {
            return check(divisionConfiguration.get(getConfigurationParameter(location), DEFAULT_QUERY_NAME));
        } catch (FxNotFoundException e) {
            return new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location);
        } catch (FxApplicationException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection<String> loadNames(ResultLocation location) throws FxApplicationException {
        final List<String> names = Lists.newArrayList(
                configuration.getKeys(getConfigurationParameter(location))
        );
        // remove default query from list
        final Iterator<String> iter = names.iterator();
        while (iter.hasNext()) {
            if (iter.next().equals(DEFAULT_QUERY_NAME)) {
                iter.remove();
            }
        }

        return names;
    }

    /**
     * Check a saved query and remove invalid nodes (happens when assignments
     * or properties of the query are no longer available).
     *
     * @param root  the query to be checked
     * @return      root
     */
    private QueryRootNode check(QueryRootNode root) {
        final List<QueryNode> invalidNodes = Lists.newArrayList();

        // collect all nodes that are no longer valid
        root.visit(new QueryNodeVisitor() {
            @Override
            public void visit(QueryOperatorNode operatorNode) {
                // nop, empty nodes will be removed automatically
            }

            @Override
            public void visit(QueryValueNode valueNode) {
                if (!valueNode.isValidInEnvironment()) {
                    invalidNodes.add(valueNode);
                }
            }

            @Override
            public void setCurrentParent(QueryOperatorNode operatorNode) {
                // nop
            }
        });

        // remove invalid nodes
        for (QueryNode node : invalidNodes) {
            root.removeChild(node);
        }
        
        return root;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void save(QueryRootNode query) throws FxApplicationException {
        save(configuration, query, query.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveDefault(QueryRootNode query) throws FxApplicationException {
        save(configuration, query, DEFAULT_QUERY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveSystemDefault(QueryRootNode query) throws FxApplicationException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException(LOG, "ex.searchQuery.systemDefault.noAccess");
        }
        save(EJBLookup.getDivisionConfigurationEngine(), query, DEFAULT_QUERY_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(ResultLocation location, String name) throws FxApplicationException {
        configuration.remove(getConfigurationParameter(location), name);
    }

    private void save(GenericConfigurationEngine configuration, QueryRootNode query, String key) throws FxUpdateException {
        try {
            configuration.put(getConfigurationParameter(query.getLocation()), key, query);
        } catch (Exception e) {
            throw new FxUpdateException(LOG, "ex.searchQuery.toXml", e, e.getMessage());
        }
    }

    /**
     * Return the appropriate string parameter for the given query type.
     *
     * @param location the result location
     * @return the appropriate string parameter for the given query type.
     */
    private Parameter<QueryRootNode> getConfigurationParameter(ResultLocation location) {
        final Parameter<QueryRootNode> parameter = SystemParameters.USER_QUERIES_CONTENT;
        // append the location name to the parameter path
        final ParameterPathBean locationPath = new ParameterPathBean(parameter.getPath().getValue() + "/" + location.getName(), parameter.getScope());
        return ParameterFactory.newInstance(QueryRootNode.class, new ParameterDataBean<QueryRootNode>(locationPath, null));
    }

}
