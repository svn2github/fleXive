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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.renderer.FxValueFormatter;

import java.util.Arrays;
import java.util.List;

/**
 * Value node for tree queries (i.e. search in subtrees).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TreeValueNode extends QueryValueNode<FxValue, TreeValueNode.TreeValueComparator> {
    private static final long serialVersionUID = -403941329020860113L;

    private final long nodeId;
    private final FxTreeMode treeMode;
    private final FxString nodeLabel;

    public static enum TreeValueComparator implements ValueComparator {
        CHILD, DIRECTCHILD;

        /** {@inheritDoc} */
        @Override
        public boolean isNeedsInput() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public FxString getLabel() {
            return FxSharedUtils.getEnumLabel(this);
        }
    }

    public TreeValueNode(int id, long nodeId, FxTreeMode treeMode, FxString nodeLabel) {
        super(id);
        this.nodeId = nodeId;
        this.comparator = TreeValueComparator.CHILD;
        this.nodeLabel = nodeLabel;
        this.treeMode = treeMode;
        setValue(this.nodeLabel);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public FxString getLabel() {
        return FxSharedUtils.getMessage(FxSharedUtils.SHARED_BUNDLE, "shared.QueryNode.label.tree");
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
            EJBLookup.getTreeEngine().getNode(treeMode, nodeId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    /** {@inheritDoc} */
    @Override
    public FxValueFormatter getValueFormatter() {
        return new FxValueFormatter() {
            @Override
            public String format(FxValue container, Object value, FxLanguage outputLanguage) {
                if (value == null) {
                    return "";
                }
                // append tree mode to label
                return "<span title=\"" + FxFormatUtils.escapeForAttribute(value.toString()) + "\">"
                        + value.toString() + " " 
                        + FxSharedUtils.getMessage(
                            FxSharedUtils.SHARED_BUNDLE,
                            "shared.QueryNode.label.tree." + (FxTreeMode.Live.equals(treeMode) ? "live" : "edit")
                        )
                        + "</span>";
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    protected void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            // TODO: add tree live mode to query
            if (this.comparator.equals(TreeValueComparator.CHILD)) {
                builder.isChild(nodeId);
            } else {
                builder.isDirectChild(nodeId);
            }
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<TreeValueComparator> getNodeComparators() {
        return Arrays.asList(TreeValueComparator.values());
    }
}
