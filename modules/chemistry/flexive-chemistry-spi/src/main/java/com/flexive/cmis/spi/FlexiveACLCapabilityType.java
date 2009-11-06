/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.cmis.spi;

import org.apache.chemistry.ACLCapabilityType;
import org.apache.chemistry.PermissionsSupported;
import org.apache.chemistry.ACLPropagation;
import org.apache.chemistry.Permission;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveACLCapabilityType implements ACLCapabilityType  {
    public PermissionsSupported getSupportedPermissions() {
        return PermissionsSupported.REPOSITORY;
    }

    public ACLPropagation getACLPropagation() {
        return ACLPropagation.OBJECT_ONLY;
    }

    public Set<Permission> getRepositoryPermissions() {
        // TODO
        return new HashSet<Permission>(
                Arrays.asList(
                        new Permission() {
                            public String getId() {
                                return Permission.ALL;
                            }
                            public String getDescription() {
                                return "";
                            }
                        }
                )
        );
    }

    public Map<String, String> getPermissionMappings() {
        // TODO
        return null;
    }
}