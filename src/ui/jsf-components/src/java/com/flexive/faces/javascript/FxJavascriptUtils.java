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
package com.flexive.faces.javascript;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;

/**
 * Utility functions for handling/creating javascript code.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxJavascriptUtils {

    /**
     * Private c'tor to avoid instantiation
     */
    private FxJavascriptUtils() {
    }

    /**
     * Start a javascript block.
     *
     * @param out the output writer
     * @throws IOException if the code could not be written
     */
    public static void beginJavascript(Writer out) throws IOException {
        out.write("<script type=\"text/javascript\">\n<!--\n");
    }

    /**
     * End a javascript block.
     *
     * @param out the output writer
     * @throws IOException if the code could not be written
     */
    public static void endJavascript(Writer out) throws IOException {
        out.write("\n//-->\n</script>");
    }

    /**
     * Write dojo "require" calls for all requested components.
     *
     * @param out      the output writer
     * @param packages array of packages to be written (e.g. "dojo.widget.*")
     * @throws IOException if the code could not be written
     */
    public static void writeDojoRequires(Writer out, String... packages) throws IOException {
        for (String pkg : packages) {
            out.write("dojo.require(\"" + pkg + "\");\n");
        }
    }

    /**
     * Converts the given fully qualified widget name (e.g. dojo.widget.Menu2) to
     * the widget name used createWidget. The package prefix "flexive.widget" is converted
     * to the namespace prefix "flexive:".
     *
     * @param fqn a fully qualified widget name, e.g. "dojo.widget.Menu2"
     * @return the widget name for dojo.widget.createWidget
     */
    public static String getWidgetName(String fqn) {
        return StringUtils.replace(StringUtils.replace(fqn, "flexive.widget.", "flexive:"), "dojo.widget.", "");
    }
}
