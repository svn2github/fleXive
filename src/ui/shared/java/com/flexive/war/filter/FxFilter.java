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
package com.flexive.war.filter;

import com.flexive.core.Database;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.timer.FxQuartz;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxExceptionMessage;
import com.flexive.shared.mbeans.MBeanHelper;
import com.flexive.shared.media.impl.FxMimeType;
import com.google.common.collect.Sets;
import com.metaparadigm.jsonrpc.JSONRPCBridge;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The main [fleXive] servlet filter. Its main responsibility is to provide the
 * {@link FxContext} instance for the current request, which can be retrieved with
 * {@link com.flexive.shared.FxContext#get()} any time during a request made through FxFilter.
 *
 * <p>
 *     Init parameters:
 * </p>
 *
 * <ul>
 *     <li><strong>excludedPaths</strong>: a list of path prefixes separated by semicolons that will completely bypass the filter (useful
 *     since FxFilter is often mapped with wide "catch-all" patterns, e.g. "/*")</li>
 * </ul>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 *
 * @version $Rev$
 */
public class FxFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(FxFilter.class);

    private static final String CATALINA_CLIENT_ABORT = "org.apache.catalina.connector.ClientAbortException";
    private static final String MSG_SKIP_CLEANUP = "Skipping cleanup since cache is already uninstalled.";
    private static final String X_POWERED_BY_VALUE = "[fleXive]";
    private static final String JSON_RPC_MARKER = "fxFilterMarker";
    private static final Object JSON_RPC_LOCK = new Object();

    private static final ConcurrentMap<Integer, Boolean> DIVISION_SERVICES = new ConcurrentHashMap<Integer, Boolean>();
    private static final String PARAM_EXCLUDED_PATHS = "excludedPaths";

    private String FILESYSTEM_WAR_ROOT;
    private FilterConfig config;
    private Set<String> excludedPaths;

    /**
     * Returns the root of the war directory on the filesystem.
     *
     * @return The root of the war directory on the filesystem.
     */
    public String getFileystemWarRoot() {
        return FILESYSTEM_WAR_ROOT;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        // Get the war deployment directory root on the server file system
        // Eg. "/opt/jboss-4.0.3RC1/server/default/./tmp/deploy/tmp52986Demo.ear-contents/web.war/"
        this.FILESYSTEM_WAR_ROOT = filterConfig.getServletContext().getRealPath("/");

        final String excluded = config.getInitParameter(PARAM_EXCLUDED_PATHS);
        if (StringUtils.isNotBlank(excluded)) {
            this.excludedPaths = Sets.newHashSet(StringUtils.split(excluded, ";'"));
            LOG.info("Excluded paths from FxFilter: " + this.excludedPaths);
        } else {
            this.excludedPaths = Collections.emptySet();
        }
    }

    @Override
    public void destroy() {
        try {
            // shutdown timer service if it is installed - cannot use EJB call here since we're shutting down
            FxQuartz.shutdown();

            // cleanup MIME detectors
            FxMimeType.shutdownDetectors();

            cleanupCache();

            EJBLookup.clearCache();

            FxExceptionMessage.cleanup();

            Database.cleanup();

            DIVISION_SERVICES.clear();

        } catch (SchedulerException ex) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to shutdown Quartz scheduler: " + ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(ex.getMessage(), ex);
            }
        } finally {
            FxContext.cleanup();
        }
    }

    private void cleanupCache() throws Exception {
        final String deploymentId;
        if (!CacheAdmin.isCacheMBeanInstalled()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(MSG_SKIP_CLEANUP);
            }
            return;
        }
        try {
            deploymentId = CacheAdmin.getInstance().getDeploymentId();
        } catch (Exception e) {
            if (LOG.isInfoEnabled()) {
                LOG.info(MSG_SKIP_CLEANUP);
            }
            return;
        }
        if (MBeanHelper.DEPLOYMENT_ID.equals(deploymentId)) {
            // Destroy local cache instance.
            // This also stops local streaming servers, since they cannot use EJB calls from a shutdown context.
            CacheAdmin.uninstallLocalInstance();
        }
    }


    /**
     * @param servletRequest  -
     * @param servletResponse -
     * @param filterChain     -
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        FxRequestUtils.setCharacterEncoding(servletRequest, servletResponse);

        final int divisionId = getDivisionId(servletRequest, servletResponse);
        if (divisionId == FxContext.DIV_UNDEFINED) {
            return;
        }

        // check excluded paths
        if (!excludedPaths.isEmpty()) {
            final HttpServletRequest outerRequest = (HttpServletRequest) servletRequest;
            final String path = outerRequest.getRequestURI().substring(outerRequest.getContextPath().length());
            for (String excluded : excludedPaths) {
                if (path.startsWith(excluded)) {
                    // skip FxFilter processing, proceed in filter chain
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }

        try {
            // Generate a FlexRequestWrapper which stores additional informations
            final FxRequestWrapper request = servletRequest instanceof FxRequestWrapper
                    ? (FxRequestWrapper) servletRequest
                    : new FxRequestWrapper((HttpServletRequest) servletRequest, divisionId);

            // create thread-local FxContext variable for the current user
            FxContext.storeInfos(request, request.isDynamicContent(), divisionId, false);

            performDivisionServices();
            initializeJsonRpc(request.getSession());

            // Cache data for jsp,jsf,and xhtml pages only
            /* TODO: fix/implements response caching.
               The 'catchData' mode doesn't work for static 
               resources like images or CSS files served by servlets, like for example Seam/Richfaces
               includes. Besides, since caching has not been implemented yet, 'catchData' doesn't do much
               except rendering our own error page when the page threw an exception.
            */
            final boolean cacheData = false; //request.isDynamicContent();

            // Wrap the response to provide additional features (content length counting, caching)
            final FxResponseWrapper response =
                    (servletResponse instanceof FxResponseWrapper)
                            ? (FxResponseWrapper) servletResponse
                            : new FxResponseWrapper(servletResponse, cacheData);

            response.setXPoweredBy(X_POWERED_BY_VALUE);
            if (request.isDynamicContent() && !response.containsHeader("Cache-Control") && !response.containsHeader("Expires")) {
                response.disableBrowserCache();
            } else if (!response.containsHeader("last-modified") || !response.containsHeader("expires")) {
                response.enableBrowserCache(FxResponseWrapper.CacheControl.PRIVATE, null, false);
            }

            filterChain.doFilter(request, response);

            try {
                if ("css".equalsIgnoreCase(request.getPageType()) &&
                        (response.getContentType() == null || !response.getContentType().startsWith("text/css"))) {
                    response.setContentType("text/css");
                } else if ("js".equalsIgnoreCase(request.getPageType()) &&
                        (response.getContentType() == null || !response.getContentType().contains("javascript"))) {
                    response.setContentType("application/x-javascript");
                }

                if (!response.isClientWriteThrough()) {
                    // Manually write the final response to the client
                    if (response.hadError()) {
                        writeErrorPage(response);
                    } else {
                        response.writeToUnderlyingResponse(null);
                    }
                } else {
                    // nothing
                }
            } catch (Exception e) {
                if (CATALINA_CLIENT_ABORT.equals(e.getClass().getCanonicalName())) {
                    LOG.debug("Client aborted transfer: " + e.getMessage(), e);
                } else {
                    LOG.error("FxFilter caught exception: " + e.getMessage(), e);
                }
            }
        } finally {
            FxContext.cleanup();
        }

    }

    private void writeErrorPage(FxResponseWrapper response) throws IOException, FxApplicationException {
        response.getWrappedResponse().reset();
        response.disableBrowserCache();
        final String error = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\" >\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "<title>[fleXive] Error Report</title>\n" +
                //(js==null?"":js)+
                "</head>\n" +
                "<body style=\"background-color:white;\">\n" +
                "<h1 style=\"color:red;\"> Error Code: " + response.getStatus() + "</h1><br/>" +
                response.getStatusMsg().replaceAll("\n", "<br/>") + "\n" +
                "</body>\n" +
                "</html>";
        response.writeToUnderlyingResponse(error);
    }

    private void performDivisionServices() {
        if (!isDivisionServicesPerformed()) {
            synchronized(FxFilter.class) {
                // install timer service
                if (!EJBLookup.getTimerService().isInstalled()) {
                    EJBLookup.getTimerService().install(true);
                }
                // patch database
                try {
                    EJBLookup.getDivisionConfigurationEngine().patchDatabase();
                } catch (FxApplicationException e) {
                    throw e.asRuntimeException();
                }
                // initialize flat storage, if available
                FxFlatStorageManager.getInstance().getDefaultStorage();
                setDivisionServicesPerformed();
            }
        }
    }

    private boolean isDivisionServicesPerformed() {
        final int divisionId = FxContext.get().getDivisionId();
        return DIVISION_SERVICES.containsKey(divisionId) && DIVISION_SERVICES.get(divisionId);
    }

    private void setDivisionServicesPerformed() {
        DIVISION_SERVICES.put(FxContext.get().getDivisionId(), true);
    }

    private void initializeJsonRpc(HttpSession session) {
        final JSONRPCBridge bridge = getJsonRpcBridge(session, true);
        try {
            if (bridge.lookupObject(JSON_RPC_MARKER) != null) {
                return;
            }
            // JSON/RPC mapping not yet initialized
            synchronized(JSON_RPC_LOCK) {
                if (bridge.lookupObject(JSON_RPC_MARKER) != null) {
                    // initialized while waiting for the lock
                    return;
                }
                registerJsonRpcObjects(bridge);
                setJsonRpcBridge(session, bridge);
                bridge.registerObject(JSON_RPC_MARKER, new Object());
            }
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Failed to initialize JSON/RPC bridge: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Registers all JSON/RPC objects for a user session. JSON/RPC providers that are not part of the
     * shared WAR module must be included dynamically (<code>Class.forName(...)</code>), since we don't
     * want to introduce compile-time dependencies.
     *
     * @param bridge    the JSON/RPC bridge instance
     */
    protected void registerJsonRpcObjects(JSONRPCBridge bridge) {
        registerJsonRpcObject(bridge, "YahooResultProvider", "com.flexive.faces.javascript.yui.YahooResultProvider");
        registerJsonRpcObject(bridge, "AutoCompleteProvider", "com.flexive.faces.javascript.AutoCompleteProvider");
        registerJsonRpcObject(bridge, "ContentProvider", "com.flexive.faces.javascript.ContentProvider");
    }

    /**
     * Dynamic registration of a Java class in the JSON/RPC bridge. If the class could not be loaded,
     * a log message will be written but no exception will be thrown.
     *
     * @param bridge    the JSON/RPC bridge
     * @param name      the name under which the bean will be addressed by JSON/RPC
     * @param className the fully qualified class name
     */
    protected void registerJsonRpcObject(JSONRPCBridge bridge, String name, String className) {
        try {
            bridge.registerObject(name, Class.forName(className).newInstance());
        } catch (ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping JSON/RPC provider "
                        + name
                        + "(" + className
                        + ") because it was not found in the classpath."
                );
            }
        } catch (IllegalAccessException e) {
            LOG.warn("Failed to create JSF JSON/RPC objects: " + e.getMessage(), e);
        } catch (InstantiationException e) {
            LOG.warn("Failed to create JSF JSON/RPC objects: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the JSON-RPC-Bridge of the current session. The RPC-Bridge holds all objects accessible
     * via JSON-RPC-Java for the current session.
     *
     * @param session   the user HTTP session
     * @param force     if true and no JSON-RPC-Bridge is set in the session, a new one will be created.
     *                  Note that you have to store the newly created bridge with a call to
     *                  {@link #setJsonRpcBridge(javax.servlet.http.HttpSession, com.metaparadigm.jsonrpc.JSONRPCBridge)}
     *                  if you want to make it available in the user session.
     * @return  the JSON-RPC-Bridge of the current session
     */
    public static JSONRPCBridge getJsonRpcBridge(HttpSession session, boolean force) {
        // don't register bridge in session, all clients get the same methodset anyway
        return JSONRPCBridge.getGlobalBridge();
    }

    /**
     * Sets the JSON-RPC-Bridge for the current session. The RPC-Bridge holds all objects accessible
     * via JSON-RPC-Java for the current session.
     *
     * @param session   the HTTP session
     * @param bridge    the bridge to be stored
     */
    public static void setJsonRpcBridge(HttpSession session, JSONRPCBridge bridge) {
        // use global JSON/RPC bridge
    }

    static int getDivisionId(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException {
        int divisionId = FxRequestUtils.getDivision((HttpServletRequest) servletRequest);
        if (FxRequestUtils.getCookie((HttpServletRequest) servletRequest, FxSharedUtils.COOKIE_FORCE_TEST_DIVISION) != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Test division cookie set - overriding division retrieved from request URI.");
            }
            divisionId = DivisionData.DIVISION_TEST;
        }
        if (divisionId == FxContext.DIV_UNDEFINED) {
            ((HttpServletResponse) servletResponse).
                    sendError(HttpServletResponse.SC_NOT_FOUND, "Division not defined or configuration error");
        }
        return divisionId;
    }

}

/*
Reminders:
TODO: If-Modified-Since (request.getDateHeader( "If-Modified-Since" ): response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
*/
