/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.search.cmis.model;

import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.google.common.collect.Lists;
import com.google.common.collect.Iterables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;

import java.util.*;

/**
 * A reference to a single table, with an optional alias.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SingleTableReference implements TableReference {
    private final String alias;
    private final List<FxType> referencedTypes;
    private final FxType baseType;

    public SingleTableReference(FxEnvironment env, String name, String alias) {
        this.alias = alias;
        final FxType type = env.getType(name);
        final List<FxType> types = new ArrayList<FxType>(type.getDerivedTypes(true));
        types.add(0, type); // root type comes first
        this.referencedTypes = Collections.unmodifiableList(types);
        this.baseType = type;
    }

    public SingleTableReference(FxEnvironment env, String name) {
        this(env, name, name);
    }

    /**
     * {@inheritDoc}
     */
    public TableReference findByAlias(String alias) {
        return this.alias.equals(alias) ? this : null;
    }

    /**
     * {@inheritDoc}
     */
    public String getAlias() {
        return alias;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getReferencedAliases() {
        return Arrays.asList(alias);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getReferencedTypes() {
        return referencedTypes;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertySecurityEnabled() {
        for (FxType type : referencedTypes) {
            if (type.usePropertyPermissions()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> resolvePropertyAssignment(FxEnvironment environment, String name) {
        final List<FxPropertyAssignment> result = Lists.newArrayList();

        // select the assignment in the root type
        final FxPropertyAssignment base = baseType.getPropertyAssignment("/" + name);
        result.add(base);
        
        // add all derived assignments
        result.addAll(base.getDerivedAssignments(environment));

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getIdFilterColumn() {
        return alias + "_id";
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionFilterColumn() {
        return alias + "_ver";
    }

    /**
     * {@inheritDoc}
     */
    public List<TableReference> getSubTables() {
        return new ArrayList<TableReference>(0);
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<TableReference> getLeafTables() {
        return new Iterable<TableReference>() {
            public Iterator<TableReference> iterator() {
                return Arrays.asList((TableReference) SingleTableReference.this).iterator();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public FxType getRootType() {
        return referencedTypes.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public void accept(TableReferenceVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return alias;
    }
}