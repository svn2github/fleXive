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
package com.flexive.war.webdav.catalina;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * Naming enumeration implementation.
 *
 * @author Remy Maucherat
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class RecyclableNamingEnumeration implements NamingEnumeration {

    // ----------------------------------------------------------- Constructors


    public RecyclableNamingEnumeration(Vector entries) {
        this.entries = entries;
        recycle();
    }

    // -------------------------------------------------------------- Variables


    /**
     * Entries.
     */
    protected Vector entries;


    /**
     * Underlying enumeration.
     */
    protected Enumeration enumeration;

    // --------------------------------------------------------- Public Methods


    /**
     * Retrieves the next element in the enumeration.
     */
    public Object next()
            throws NamingException {
        return nextElement();
    }


    /**
     * Determines whether there are any more elements in the enumeration.
     */
    public boolean hasMore()
            throws NamingException {
        return enumeration.hasMoreElements();
    }


    /**
     * Closes this enumeration.
     */
    public void close()
            throws NamingException {
    }


    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }


    public Object nextElement() {
        return enumeration.nextElement();
    }

    // -------------------------------------------------------- Package Methods


    /**
     * Recycle.
     */
    void recycle() {
        enumeration = entries.elements();
    }

}


