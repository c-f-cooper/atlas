/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.omrs.localrepository.repositorycontentmanager;

import org.apache.atlas.omrs.ffdc.exception.PatchErrorException;
import org.apache.atlas.omrs.ffdc.exception.TypeErrorException;
import org.apache.atlas.omrs.metadatacollection.properties.instances.InstanceType;
import org.apache.atlas.omrs.metadatacollection.properties.typedefs.AttributeTypeDef;
import org.apache.atlas.omrs.metadatacollection.properties.typedefs.TypeDef;
import org.apache.atlas.omrs.metadatacollection.properties.typedefs.TypeDefPatch;

/**
 * OMRSTypeDefHelper provides methods for manipulating TypeDefs and creating metadata instances with the correct TypeDef
 * headers
 */
public interface OMRSTypeDefHelper
{
    /**
     * Return the TypeDef identified by the name supplied by the caller.  This is used in the connectors when
     * validating the actual types of the repository with the known open metadata types - looking specifically
     * for types of the same name but with different content.
     *
     * @param sourceName - source of the request (used for logging)
     * @param typeDefName - unique name for the TypeDef
     * @return TypeDef object or null if TypeDef is not known.
     */
    TypeDef  getTypeDefByName (String    sourceName,
                               String    typeDefName);


    /**
     * Return the AttributeTypeDef identified by the name supplied by the caller.  This is used in the connectors when
     * validating the actual types of the repository with the known open metadata types - looking specifically
     * for types of the same name but with different content.
     *
     * @param sourceName - source of the request (used for logging)
     * @param attributeTypeDefName - unique name for the TypeDef
     * @return AttributeTypeDef object or null if AttributeTypeDef is not known.
     */
    AttributeTypeDef getAttributeTypeDefByName (String    sourceName,
                                                String    attributeTypeDefName);


    /**
     * Return the TypeDef identified by the guid and name supplied by the caller.  This call is used when
     * retrieving a type that should exist.  For example, retrieving the type of a metadata instance.
     *
     * @param sourceName - source of the request (used for logging)
     * @param typeDefGUID - unique identifier for the TypeDef
     * @param typeDefName - unique name for the TypeDef
     * @return TypeDef object
     * @throws TypeErrorException - unknown or invalid type
     */
    TypeDef  getTypeDef (String    sourceName,
                         String    typeDefGUID,
                         String    typeDefName) throws TypeErrorException;


    /**
     * Return the AttributeTypeDef identified by the guid and name supplied by the caller.  This call is used when
     * retrieving a type that should exist.  For example, retrieving the type definition of a metadata instance's
     * property.
     *
     * @param sourceName - source of the request (used for logging)
     * @param attributeTypeDefGUID - unique identifier for the AttributeTypeDef
     * @param attributeTypeDefName - unique name for the AttributeTypeDef
     * @return TypeDef object
     * @throws TypeErrorException - unknown or invalid type
     */
    AttributeTypeDef  getAttributeTypeDef (String    sourceName,
                                           String    attributeTypeDefGUID,
                                           String    attributeTypeDefName) throws TypeErrorException;


    /**
     * Returns an updated TypeDef that has had the supplied patch applied.  It throws an exception if any part of
     * the patch is incompatible with the original TypeDef.  For example, if there is a mismatch between
     * the type or version that either represents.
     *
     * @param sourceName - source of the request (used for logging)
     * @param typeDefPatch - patch to apply
     * @return updated TypeDef
     * @throws PatchErrorException - the patch is either badly formatted, or does not apply to the supplied TypeDef
     */
    TypeDef   applyPatch(String   sourceName, TypeDefPatch typeDefPatch) throws PatchErrorException;
}
