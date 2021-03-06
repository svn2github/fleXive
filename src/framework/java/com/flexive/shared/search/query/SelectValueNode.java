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
package com.flexive.shared.search.query;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.FxSelectOne;

import java.util.Arrays;
import java.util.List;

/**
 * Select node
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SelectValueNode extends QueryValueNode<FxSelectOne, PropertyValueComparator> {
    private static final long serialVersionUID = 251709443170235315L;
    private final FxAssignment assignment;

    public SelectValueNode(int id, FxAssignment assignment, FxSelectListItem item) {
        super(id);
        if (item == null || item.getList() == null) {
            throw new FxInvalidParameterException("ITEM", "ex.queryNode.select.empty", this).asRuntimeException();
        }
        this.assignment = assignment;
        this.comparator = PropertyValueComparator.EQ;
        setValue(new FxSelectOne(item));
    }

    /** {@inheritDoc} */
    @Override
    public List<PropertyValueComparator> getNodeComparators() {
        return Arrays.asList(PropertyValueComparator.EQ, PropertyValueComparator.NE);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidInEnvironment() {
        try {
            CacheAdmin.getEnvironment().getAssignment(assignment.getId());
            return true;
        } catch (FxRuntimeException e) {
            return false;
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            builder.condition(assignment, getComparator(), getValue());
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException();
        }
    }
}
