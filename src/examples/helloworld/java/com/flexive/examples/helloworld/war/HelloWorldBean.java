/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.examples.helloworld.war;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.SqlQueryBuilder;

import javax.faces.model.DataModel;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class HelloWorldBean {
    private DataModel blogEntries;

    public DataModel getBlogEntries() throws FxApplicationException {
        if (blogEntries == null) {
            final FxResultSet result = new SqlQueryBuilder()
                    .select("@pk", "entryTitle", "entryText", "created_at")
                    .type("blogEntry")
                    .orderBy("created_at", SortDirection.DESCENDING)
                    .getResult();
            blogEntries = new FxResultSetDataModel(result);
        }
        return blogEntries;
    }
}
