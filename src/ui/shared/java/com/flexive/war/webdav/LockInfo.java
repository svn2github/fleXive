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
package com.flexive.war.webdav;

import com.flexive.war.webdav.catalina.FastHttpDateFormat;
import com.flexive.war.webdav.catalina.XMLWriter;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Holds a lock information.
 */
class LockInfo {

    static final int INFINITY = 3;

    /**
     * Constructor.
     */
    public LockInfo() {
    }

    String path = "/";
    String type = "write";
    String scope = "exclusive";
    int depth = 0;
    String owner = "";
    Vector<String> tokens = new Vector<String>();
    long expiresAt = 0;
    Date creationDate = new Date();


    /**
     * Get a String representation of this lock token.
     */
    public String toString() {

        String result = "Type:" + type + "\n";
        result += "Scope:" + scope + "\n";
        result += "Depth:" + depth + "\n";
        result += "Owner:" + owner + "\n";
        result += "Expiration:" + FastHttpDateFormat.formatDate(expiresAt, null) + "\n";
        Enumeration tokensList = tokens.elements();
        while (tokensList.hasMoreElements()) {
            result += "Token:" + tokensList.nextElement() + "\n";
        }
        return result;

    }


    /**
     * Return true if the lock has expired.
     */
    public boolean hasExpired() {
        return (System.currentTimeMillis() > expiresAt);
    }


    /**
     * Return true if the lock is exclusive.
     */
    public boolean isExclusive() {
        return (scope.equals("exclusive"));
    }


    /**
     * Get an XML representation of this lock token. This method will
     * append an XML fragment to the given XML writer.
     */
    public void toXML(XMLWriter generatedXML) {

        generatedXML.writeElement(null, "activelock", XMLWriter.OPENING);

        generatedXML.writeElement(null, "locktype", XMLWriter.OPENING);
        generatedXML.writeElement(null, type, XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "locktype", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "lockscope", XMLWriter.OPENING);
        generatedXML.writeElement(null, scope, XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "lockscope", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "depth", XMLWriter.OPENING);
        if (depth == INFINITY) {
            generatedXML.writeText("Infinity");
        } else {
            generatedXML.writeText("0");
        }
        generatedXML.writeElement(null, "depth", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "owner", XMLWriter.OPENING);
        generatedXML.writeText(owner);
        generatedXML.writeElement(null, "owner", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "timeout", XMLWriter.OPENING);
        long timeout = (expiresAt - System.currentTimeMillis()) / 1000;
        generatedXML.writeText("Second-" + timeout);
        generatedXML.writeElement(null, "timeout", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "locktoken", XMLWriter.OPENING);
        Enumeration tokensList = tokens.elements();
        while (tokensList.hasMoreElements()) {
            generatedXML.writeElement(null, "href", XMLWriter.OPENING);
            generatedXML.writeText("opaquelocktoken:"
                    + tokensList.nextElement());
            generatedXML.writeElement(null, "href", XMLWriter.CLOSING);
        }
        generatedXML.writeElement(null, "locktoken", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "activelock", XMLWriter.CLOSING);

    }
}