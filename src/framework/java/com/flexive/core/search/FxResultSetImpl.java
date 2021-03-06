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
package com.flexive.core.search;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.*;
import com.flexive.sqlParser.FxStatement;
import com.flexive.sqlParser.SelectedValue;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * FxResultSet implementation
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxResultSetImpl implements Serializable, FxResultSet {
    private static final long serialVersionUID = -7038323618490882790L;

    private final List<Object[]> rows;
    private final String[] columnNames;
    private final int parserExecutionTime;
    private final int dbSearchTime;
    private final int startIndex;
    private final int maxFetchRows;
    private final ResultLocation location;
    private final ResultViewType viewType;
    private final List<FxFoundType> contentTypes;
    private final long creationTime;
    private final long createdBriefcaseId;
    private final long typeId;

    private int fetchTime = 0;
    private int totalTime = 0;
    private int totalRowCount;
    private boolean truncated;
    private Map<String, Integer> columnIndexMap;
    private int userWildcardIndex = -1;

    private transient int pkIndex = -2;

    // cached properties
    private transient String[] columnLabels;
    private transient Map<String, Integer> columnNameLookup;

    private class RowIterator implements Iterator<FxResultRow> {
        int index = 0;

        @Override
        public boolean hasNext() {
            return index < rows.size();
        }

        @Override
        public FxResultRow next() {
            return new FxResultRow(FxResultSetImpl.this, index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removing rows not supported");
        }
    }

    protected FxResultSetImpl(final FxStatement fx_stmt, final int parserExecutionTime, int dbSearchTime,
                              int startIndex, int maxFetchRows, ResultLocation location, ResultViewType viewType,
                              List<FxFoundType> types, long typeId, long createdBriefcaseId) {
        this.parserExecutionTime = parserExecutionTime;
        this.startIndex = startIndex;
        this.maxFetchRows = maxFetchRows;
        this.dbSearchTime = dbSearchTime;
        this.location = location;
        this.viewType = viewType;
        this.contentTypes = types == null ? new ArrayList<FxFoundType>(0) : types;
        this.createdBriefcaseId = createdBriefcaseId;
        this.rows = new ArrayList<Object[]>();
        this.columnNames = new String[fx_stmt.getSelectedValues().size()];
        int pos = 0;
        for (SelectedValue v : fx_stmt.getSelectedValues()) {
            this.columnNames[pos++] = v.getAlias();
        }
        this.totalTime = parserExecutionTime;
        this.creationTime = System.currentTimeMillis();
        this.typeId = typeId;
    }

    private void readObject(ObjectInputStream os) throws ClassNotFoundException, IOException {
        os.defaultReadObject();
        pkIndex = -2;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getStartIndex() {
        return startIndex;
    }


    /**
     * {@inheritDoc} *
     */
    @Override
    public int getMaxFetchRows() {
        return maxFetchRows;
    }

    protected void setFetchTime(int fetchTime) {
        this.fetchTime = fetchTime;
    }

    protected void addRow(Object[] rowData) {
        rows.add(rowData);
    }

    protected void setTotalTime(int executeQueryTime) {
        this.totalTime = executeQueryTime + parserExecutionTime;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String[] getColumnNames() {
        return this.columnNames.clone();
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getColumnIndex(String name) {
        if (columnNameLookup == null || !columnNameLookup.containsKey(name)) {
            if (columnNameLookup == null) {
                columnNameLookup = Maps.newHashMap();
            }
            columnNameLookup.put(name, FxSharedUtils.getColumnIndex(columnNames, name));
        }
        return columnNameLookup.get(name);
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String getColumnLabel(int index) throws ArrayIndexOutOfBoundsException {
        return getColumnLabels()[index - 1];
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String[] getColumnLabels() {
        if (columnLabels == null) {
            columnLabels = new String[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                String name = columnNames[i];
                if (name.indexOf('(') > 0) {
                    // strip function calls
                    name = name.substring(name.lastIndexOf('('), name.indexOf(')'));
                }
                if (name.indexOf('.') > 0) {
                    // strip table column
                    name = name.substring(0, name.indexOf('.'));
                }
                if (name.indexOf('@') != -1) {
                    columnLabels[i] = FxSharedUtils.getMessage(
                            FxSharedUtils.SHARED_BUNDLE,
                            "shared.result.columns." + name
                    ).toString();
                } else {
                    String label;
                    try {
                        label = CacheAdmin.getEnvironment().getAssignment(name).getLabel().getBestTranslation();
                    } catch (FxRuntimeException e) {
                        // assignment not found, try property
                        try {
                            label = CacheAdmin.getEnvironment().getProperty(name).getLabel().getBestTranslation();
                        } catch (FxRuntimeException e2) {
                            label = name;
                        }
                    }
                    columnLabels[i] = label;
                }
            }
        }
        return columnLabels.clone();
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public Map<String, Integer> getColumnIndexMap() {
        if (columnIndexMap == null) {
            columnIndexMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, Integer>() {
                private static final long serialVersionUID = 5832530872300639732L;

                @Override
                public Integer get(Object key) {
                    return getColumnIndex(String.valueOf(key));
                }
            }, true);
        }
        return columnIndexMap;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public List<Object[]> getRows() {
        return rows == null ? new ArrayList<Object[]>(0) : rows;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String getColumnName(int pos) throws ArrayIndexOutOfBoundsException {
        try {
            return this.columnNames[pos - 1];
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";pos:" + pos);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getColumnCount() {
        return this.columnNames.length;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getTotalRowCount() {
        // total row count not selected, return size of result set
        if (totalRowCount == -1) {
            return getRowCount();
        }
        // no type filter specified, return global total row count
        if (typeId == -1) {
            return totalRowCount;
        }
        // type filter specified, find total rows for the current type
        for (FxFoundType contentType : contentTypes) {
            if (contentType.getContentTypeId() == typeId) {
                return contentType.getFoundEntries();
            }
        }
        // type filter specified, but no content found
        return 0;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public Object getObject(int rowIndex, int columnIndex) throws ArrayIndexOutOfBoundsException {

        // Access row data
        Object rowData[];
        try {
            rowData = this.rows.get(rowIndex - 1);
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";rowIndex:" + rowIndex);
        }

        // Access column data
        try {
            return rowData[columnIndex - 1];
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";columnIndex:" + columnIndex);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumn(int columnIndex) {
        final List<T> result = new ArrayList<T>(rows.size());
        if (rows.size() == 0) {
            return result;
        }
        checkColumnIndex(columnIndex);
        for (Object[] row : rows) {
            result.add((T) row[columnIndex - 1]);
        }
        return result;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String getString(int rowIndex, int columnIndex) throws ArrayIndexOutOfBoundsException {
        Object value = getObject(rowIndex, columnIndex);
        if (value instanceof String) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getParserExecutionTime() {
        return parserExecutionTime;
    }

    /**
     * Returns the time needed to find all matching records in the database.
     *
     * @return the time needed to find all matching records in the database
     */
    @Override
    public int getDbSearchTime() {
        return dbSearchTime;
    }

    /**
     * Returns the time needed to find fetch the matching records from the database.
     *
     * @return the time needed to find fetch the matching records from the database
     */
    @Override
    public int getFetchTime() {
        return fetchTime;
    }

    /**
     * Returns the total time spent for the search.
     * <p/>
     * This time includes the parse time, search time, fetch time and additional
     * programm logic time.
     *
     * @return the total time spent for the search
     */
    @Override
    public int getTotalTime() {
        return totalTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultLocation getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultViewType getViewType() {
        return viewType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxFoundType> getContentTypes() {
        return contentTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<FxResultRow> getResultRows() {
        return new Iterable<FxResultRow>() {
            @Override
            public Iterator<FxResultRow> iterator() {
                return new RowIterator();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxResultRow getResultRow(int index) {
        return new FxResultRow(this, index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxResultRow getResultRow(FxPK pk) {
        if (getPrimaryKeyIndex() != -1) {
           RowIterator iterator = new RowIterator();
           while (iterator.hasNext()) {
               FxResultRow row = iterator.next();
               if (row.getPk(getPrimaryKeyIndex()).equals(pk))
                   return row;
           }
       }
       return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreatedBriefcaseId() {
        return createdBriefcaseId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUserWildcardIndex() {
        return userWildcardIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPrimaryKeyIndex() {
        if (pkIndex == -2) {
            int index = 1;
            for (String columnName : columnNames) {
                if ("@pk".equalsIgnoreCase(columnName)) {
                    pkIndex = index;
                    break;
                }
                index++;
            }
            if (pkIndex == -2) {
                pkIndex = -1;
            }
        }
        return pkIndex;
    }

    public void setUserWildcardIndex(int userWildcardIndex) {
        this.userWildcardIndex = userWildcardIndex;
    }

    protected void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    protected void setTotalRowCount(int totalRowCount) {
        this.totalRowCount = totalRowCount;
    }

    /**
     * Checks the 1-based column index and throws a runtime exception when it is not valid in this result set.
     *
     * @param columnIndex   the 1-based column index
     */
    private void checkColumnIndex(int columnIndex) {
        if (rows.size() == 0) {
            return;
        }
        if (rows.get(0).length < columnIndex || columnIndex < 1) {
            throw new FxInvalidParameterException("columnIndex", "ex.sqlSearch.column.index", columnIndex, rows.get(0).length).asRuntimeException();
        }
    }

}
