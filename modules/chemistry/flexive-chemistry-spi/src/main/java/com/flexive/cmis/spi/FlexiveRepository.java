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
package com.flexive.cmis.spi;

import com.flexive.chemistry.webdav.extensions.CopyDocumentExtension;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxType;
import com.google.common.collect.Lists;
import org.apache.chemistry.*;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.flexive.shared.CacheAdmin.getEnvironment;

/**
 * The flexive repository implementation.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveRepository implements Repository {

    private final FlexiveRepositoryConfig config;

    /**
     * @param contentStreamURI  the base URI for content streams.
     */
    public FlexiveRepository(String contentStreamURI) {
        this.config = new FlexiveRepositoryConfig(contentStreamURI);
    }

    public Connection getConnection(Map<String, Serializable> parameters) {
        return new FlexiveConnection(config, parameters);
    }

    public SPI getSPI(Map<String, Serializable> params) {
        return new FlexiveConnection(config, null);
    }

    public <T> T getExtension(Class<T> klass) {
        if (klass == CopyDocumentExtension.class) {
            return klass.cast(CopyDocument.getInstance());
        }
        return null;
    }

    public RepositoryInfo getInfo() {
        return new FlexiveRepositoryInfo();
    }

    public Collection<Type> getTypes() {
        final List<FxType> fxTypes = getEnvironment().getTypes();
        final List<Type> result = Lists.newArrayListWithCapacity(fxTypes.size());
        for (FxType fxType : fxTypes) {
            result.add(new FlexiveType(fxType));
        }
        // add base types
        result.add(new FlexiveType(BaseType.DOCUMENT.getId()));
        result.add(new FlexiveType(BaseType.FOLDER.getId()));
        // TODO: relationship and policy types
        
        return result;
    }

    public ListPage<Type> getTypeChildren(String typeId, boolean includePropertyDefinitions, Paging paging) {
        return ListPageUtils.page(
                getTypes(typeId, false),
                paging,
                ListPageUtils.<Type>identityFunction()
        );
    }

    public Collection<Type> getTypeDescendants(String typeId) {
        return getTypes(typeId, true);
    }

    public Collection<Type> getTypeDescendants(String typeId, int depth, boolean includePropertyDefinitions) {
        // TODO: use depth/includePropertyDefinitions
        return getTypes(typeId, true);
    }

    private Collection<Type> getTypes(String typeId, boolean includeType) {
        final List<Type> result = Lists.newArrayList();
        if (typeId == null) {
            return getTypes();
        } else {
            if (FlexiveType.ROOT_TYPE_ID.equalsIgnoreCase(typeId)) {
                return getTypes();
            } else {
                // return type + descendants
                for (FxType derived : getEnvironment().getType(SPIUtils.getFxTypeName(typeId)).getDerivedTypes(true, includeType)) {
                    result.add(new FlexiveType(derived));
                }
            }
        }
        return result;
    }

    public Type getType(String typeId) {
        return typeId != null ? new FlexiveType(typeId) : new FlexiveType(FxType.ROOT_ID);
    }

    public PropertyDefinition getPropertyDefinition(String id) {
        final VirtualProperties vprop = VirtualProperties.getByName(id);
        if (vprop != null) {
            return vprop.getDefinition();
        }
        return new FlexivePropertyDefinition(
                CacheAdmin.getEnvironment().getPropertyAssignment(id)
        );
    }


    public String getId() {
        return getInfo().getId();
    }

    public String getName() {
        return getInfo().getName();
    }

    public URI getThinClientURI() {
        return null;
    }

    public void addType(Type type) {
        throw new UnsupportedOperationException();
    }


    /**
     * Extension to provide efficient copying of contents.
     */
    private static class CopyDocument implements CopyDocumentExtension {
        private static final CopyDocument INSTANCE = new CopyDocument();

        private CopyDocument() {
        }

        private static CopyDocument getInstance() {
            return INSTANCE;
        }

        public void copy(Connection conn, ObjectId id, ObjectId targetFolder, String newName, boolean overwrite, boolean shallow) throws UpdateConflictException, NameConstraintViolationException {
            final CMISObject object = conn.getObject(id);
            if (!(object instanceof FlexiveObjectEntry)) {
                throw new IllegalArgumentException("Cannot copy object of type " + object.getClass());
            }
            if (StringUtils.isBlank(newName)) {
                throw new IllegalArgumentException("New resource name must not be null.");
            }

            final FlexiveObjectEntry doc = (FlexiveObjectEntry) object;
            final FlexiveFolder target = (FlexiveFolder) conn.getObject(targetFolder);

            // remove existing entry when overwrite is set
            if (overwrite) {
                for (CMISObject child : target.getChildren()) {
                    if (newName.equals(child.getName())) {
                        // remove old object
                        if (child instanceof Document) {
                            ((Document) child).deleteAllVersions();
                        } else {
                            child.delete();
                        }
                    }
                }
            }

            if (doc instanceof FlexiveFolder) {
                // perform efficient copy through the tree engine
                ((FlexiveFolder) doc).copyTo((FlexiveFolder) conn.getObject(targetFolder), newName, !shallow);
            } else {
                // link object to new folder
                final FlexiveObjectEntry copy = ((FlexiveDocument) doc).copy();
                copy.setName(newName);
                target.add(copy);
                copy.save();
                target.save();
            }
        }
    }
}
