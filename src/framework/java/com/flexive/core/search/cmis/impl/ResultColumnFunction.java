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
package com.flexive.core.search.cmis.impl;

import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.ColumnValueFunction;

/**
 * A result column wrapped in a function call (e.g. UPPER(...)).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class ResultColumnFunction extends AbstractResultColumn<ColumnReference, ResultColumnFunction> {
    private final ColumnValueFunction function;
    private final ResultColumnReference resultColumn;

    public ResultColumnFunction(ColumnValueFunction function, ResultColumnReference resultColumn) {
        assert function != null;
        assert resultColumn != null;
        this.function = function;
        this.resultColumn = resultColumn;
    }

    @Override
    protected ResultColumnFunction getThis() {
        return this;
    }

    @Override
    protected ResultColumnMapper<ResultColumnFunction> getSqlMapper(SqlMapperFactory factory) {
        return factory.selectColumnFunction();
    }

    @Override
    public ColumnReference getSelectedObject() {
        return resultColumn.getSelectedObject();
    }

    @Override
    public String getAlias() {
        return function.getAlias();
    }

    public ColumnValueFunction getFunction() {
        return function;
    }

    public ResultColumnReference getResultColumn() {
        return resultColumn;
    }
}
