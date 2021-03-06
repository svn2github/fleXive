/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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

import com.flexive.shared.FxContext;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.war.FxRequest;
import org.apache.commons.lang.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles security checks for the backend administration - checks if the calling user has the role
 * {@link Role#BackendAccess}. Must be called
 * <i>after</i> the main FxFilter in the filter chain.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class BackendAuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nop
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // get URI without application context path
        final UserTicket ticket = FxContext.getUserTicket();
        if (ticket.isGuest() && servletRequest instanceof FxRequest) {
            final FxRequest request = (FxRequest) servletRequest;
            if (request.isDynamicContent()) {
                // not logged in at all - forward to login page
                FilterUtils.sendRedirect(servletRequest, servletResponse,
                        "/pub/login.jsf"
                        // guess whether we got a session timeout (from within the backend) or a new request
                        + (StringUtils.indexOf(((HttpServletRequest) servletRequest).getHeader("Referer"), "/adm/") != -1
                                ? "?sessionExpired=true" : "")
                );
            } else {
                // static content (e.g. images), forbid access
                ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if (!ticket.isInRole(Role.BackendAccess)) {
            // logged in, but lacks role for backend access - show error page
            servletRequest.getRequestDispatcher("/pub/backendRestricted.jsf").forward(servletRequest, servletResponse);
        } else {
            // proceed
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
    }
}
