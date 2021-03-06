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
package com.flexive.shared.search;

import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Container for all supported FxSQL functions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class FxSQLFunctions {
    public static final String FUNCTION_CUSTOM_SELECT = "CUSTOM_SELECT";

    private static final Map<String, FxSQLFunction> FUNCTIONS = new HashMap<String, FxSQLFunction>();
    static {
        for (FxSQLFunction[] values: Arrays.asList(new FxSQLFunction[][] {
                DateFunction.values()
        })) {
            for (FxSQLFunction function: values) {
                if (FUNCTIONS.put(function.getSqlName().toLowerCase(), function) != null) {
                    throw new IllegalStateException("Duplicate FxSQL function mapping for '" + function.getSqlName() + "'");
                }
            }
        }
        FUNCTIONS.put(FUNCTION_CUSTOM_SELECT.toLowerCase(), new FxSQLFunction() {
            @Override
            public String getSqlName() {
                return FUNCTION_CUSTOM_SELECT;
            }

            @Override
            public FxDataType getOverrideDataType() {
                return FxDataType.Text;
            }
        });
    }

    /**
     * Prevent instantiation.
     */
    private FxSQLFunctions() {
    }

    /**
     * Returns the {@link FxSQLFunction} object mapped to the given function name. If name does not
     * represent a known FxSQL function, a FxRuntimeException is thrown.
     *
     * @param name      the function name to be  looked up
     * @return          the {@link FxSQLFunction} object
     */
    public static FxSQLFunction forName(String name) {
        return asFunctions(Arrays.asList(name)).get(0);
    }

    /**
     * Converts the given function names to {@link FxSQLFunction} objects. If an entry does not
     * represent a known FxSQL function, a FxRuntimeException is thrown.
     *
     * @param values    the function names to be converted
     * @return          the {@link FxSQLFunction} objects
     */
    public static List<FxSQLFunction> asFunctions(Collection<String> values) {
        final List<FxSQLFunction> result = new ArrayList<FxSQLFunction>(values.size());
        for (String value: values) {
            if (!FUNCTIONS.containsKey(value.toLowerCase())) {
                throw new FxNotFoundException("ex.sqlSearch.function.notFound", value,
                        StringUtils.join(getSqlNames(FUNCTIONS.values()), ", ")).asRuntimeException();
            }
            result.add(FUNCTIONS.get(value.toLowerCase()));
        }
        return result;
    }

    /**
     * Return the FxSQL function names for the given list of {@link com.flexive.shared.search.FxSQLFunction}
     * objects, i.e. project the result of {@link FxSQLFunction#getSqlName()}.
     *
     * @param functions the FxSQL function object
     * @return  the FxSQL function names as used in FxSQL
     */
    public static List<String> getSqlNames(Collection<? extends FxSQLFunction> functions) {
        final List<String> result = new ArrayList<String>(functions.size());
        for (FxSQLFunction function : functions) {
            result.add(function.getSqlName());
        }
        return result;
    }
}
