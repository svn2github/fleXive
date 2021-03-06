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
package com.flexive.core.search.cmis.model;

import com.flexive.core.search.PropertyEntry;
import com.flexive.core.search.PropertyResolver;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.storage.ContentStorage;
import com.flexive.shared.cmis.CmisVirtualProperty;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayListWithCapacity;

/**
 * This class represents a reference to a column in a CMIS-SQL query. In FxSQL, this would be a property selector.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class ColumnReference extends ValueExpression<ColumnReference> {
    private final TableReference tableReference;
    private final List<FxPropertyAssignment> assignments;
    private final PropertyEntry propertyEntry;
    private final CmisVirtualProperty cmisProperty;
    private final boolean multivalued;
    private final boolean multilanguage;

    public ColumnReference(FxEnvironment environment, ContentStorage storage, TableReference tableReference,
                           String column, String alias, boolean useUpperCase) {
        super(alias != null ? alias : column);
        this.tableReference = tableReference;

        this.cmisProperty = CmisVirtualProperty.getByCmisName(column);
        if (cmisProperty != null) {
            if (cmisProperty.isVirtualFxProperty()) {
                final String[] columns = PropertyEntry.Type.createForProperty(cmisProperty.getFxPropertyName()).getReadColumns();
                if (columns.length > 0 && StringUtils.isNotBlank(columns[0])) {
                    this.assignments = Arrays.asList(
                            environment.getPropertyAssignment("ROOT/" + columns[0])
                    );
                } else {
                    this.assignments = newArrayListWithCapacity(0);
                }
            } else if (cmisProperty.isFxProperty()) {
                // mapped flexive property, e.g. cmis:TypeId
                this.assignments = Collections.unmodifiableList(
                        tableReference.resolvePropertyAssignment(environment, column)
                );
            } else if (cmisProperty.isSupportsQuery()) {
                // virtual property, e.g. cmis:ParentId
                this.assignments = newArrayListWithCapacity(0);
            } else {
                throw new IllegalArgumentException("CMIS property not supported in queries: " + cmisProperty);
            }
        } else {
            this.assignments = Collections.unmodifiableList(
                    tableReference.resolvePropertyAssignment(environment, column)
            );
        }

        if (assignments.isEmpty()) {
            if (cmisProperty == null) {
                throw new IllegalArgumentException("No assignments returned, but no CMIS property selected.");
            }
            if (!cmisProperty.isVirtualFxProperty()) {
                // can happen when a CMIS base property is not available in this content instance.
                // Currently we ignore this, since a few CMIS properties like NAME are not mandatory,
                // but just returning null in this case seems preferable
                multivalued = false;
                multilanguage = false;
                propertyEntry = null;
            } else {
                // virtual property without an assignment (like @path)
                propertyEntry = PropertyEntry.Type.createForProperty(cmisProperty.getFxPropertyName());
                assert propertyEntry != null;
                multilanguage = false;
                multivalued = false;
            }
        } else {
            // multivalued properties have a maximum multiplicity > 1
            multivalued = assignments.get(0).getMultiplicity().getMax() > 1;
            boolean mlang = false;
            for (FxPropertyAssignment assignment : assignments) {
                if (assignment.isMultiLang()) {
                    mlang = true;
                    break;
                }
            }
            multilanguage = mlang;

            // use the base assignment for the read columns - the data type cannot vary in derived assignments
            final List<String> readColumns = new ArrayList<String>();
            readColumns.addAll(Arrays.asList(
                    PropertyEntry.getReadColumns(storage, assignments.get(0).getProperty())
            ));

            // sub-assignments will always have the same read columns etc. as our base assignment,
            // so use the base assignment to determine database mapper information like read columns
            final FxPropertyAssignment assignment = assignments.get(0);
            final String upperCaseColumn = storage.getUppercaseColumn(assignment.getProperty());
            final PropertyResolver.Table table;
            final String filterColumn;
            if (assignment.isFlatStorageEntry()) {
                table = PropertyResolver.Table.T_CONTENT_DATA_FLAT;
                filterColumn = assignment.getFlatStorageMapping().getColumn();
            } else {
                table = PropertyResolver.Table.forTableName(storage.getTableName(assignment.getProperty()));
                filterColumn = useUpperCase && upperCaseColumn != null ? upperCaseColumn : readColumns.get(0);
            }
            if (cmisProperty != null && cmisProperty.isVirtualFxProperty()) {
                propertyEntry = PropertyEntry.Type.createForProperty(cmisProperty.getFxPropertyName());
            } else {
                propertyEntry = new PropertyEntry(PropertyEntry.Type.PROPERTY_REF,
                        table,
                        assignment,
                        readColumns.toArray(new String[readColumns.size()]),
                        filterColumn,
                        assignment.isMultiLang(), null
                );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> getReferencedAssignments() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxPropertyAssignment getBaseAssignment() {
        return assignments.isEmpty() ? null : assignments.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableReference getTableReference() {
        return tableReference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionColumnMapper<? super ColumnReference> getConditionColumnMapper(SqlMapperFactory factory) {
        return factory.filterColumnReference();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyResolver.Table getFilterTableType() {
        return propertyEntry.getTableType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilterTableName() {
        switch(getFilterTableType()) {
            case T_CONTENT_DATA_FLAT:
                return assignments.get(0).getFlatStorageMapping().getStorage();
            default:
                return getFilterTableType().getTableName();
        }
    }

    public PropertyEntry getPropertyEntry() {
        return propertyEntry;
    }

    public CmisVirtualProperty getCmisProperty() {
        return cmisProperty;
    }

    @Override
    public String toString() {
        return (tableReference != null ? tableReference.toString() + "." : "") + getAlias();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultivalued() {
        return multivalued;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultilanguage() {
        return multilanguage;
    }
}
