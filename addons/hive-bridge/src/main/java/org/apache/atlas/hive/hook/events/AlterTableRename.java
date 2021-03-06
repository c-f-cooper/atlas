/**
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

package org.apache.atlas.hive.hook.events;

import org.apache.atlas.hive.hook.AtlasHiveHookContext;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityPartialUpdateRequestV2;
import org.apache.atlas.model.notification.HookNotification.EntityUpdateRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hive.ql.hooks.Entity;
import org.apache.hadoop.hive.ql.hooks.WriteEntity;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AlterTableRename extends BaseHiveEvent {
    private static final Logger LOG = LoggerFactory.getLogger(AlterTableRename.class);

    public AlterTableRename(AtlasHiveHookContext context) {
        super(context);
    }

    @Override
    public List<HookNotification> getNotificationMessages() throws Exception {
        List<HookNotification> ret = new ArrayList<>();

        if (CollectionUtils.isEmpty(getHiveContext().getInputs())) {
            LOG.error("AlterTableRename: old-table not found in inputs list");

            return ret;
        }

        Table oldTable  = getHiveContext().getInputs().iterator().next().getTable();
        Table newTable = null;

        if (CollectionUtils.isNotEmpty(getHiveContext().getOutputs())) {
            for (WriteEntity entity : getHiveContext().getOutputs()) {
                if (entity.getType() == Entity.Type.TABLE) {
                    newTable = entity.getTable();

                    //Hive sends with both old and new table names in the outputs which is weird. So skipping that with the below check
                    if (StringUtils.equalsIgnoreCase(newTable.getDbName(), oldTable.getDbName()) && StringUtils.equalsIgnoreCase(newTable.getTableName(), oldTable.getTableName())) {
                        newTable = null;

                        continue;
                    }

                    break;
                }
            }
        }

        if (newTable == null) {
            LOG.error("AlterTableRename: renamed table not found in outputs list");

            return ret;
        }

        AtlasEntityWithExtInfo oldTableEntity = toTableEntity(oldTable);

        // first update with oldTable info, so that the table will be created if it is not present in Atlas
        ret.add(new EntityUpdateRequestV2(getUserName(), new AtlasEntitiesWithExtInfo(oldTableEntity)));

        // update qualifiedName for all columns, partitionKeys, storageDesc
        String newTableQualifiedName = getQualifiedName(newTable);

        renameColumns((List<AtlasObjectId>) oldTableEntity.getEntity().getAttribute(ATTRIBUTE_COLUMNS), oldTableEntity, newTableQualifiedName, ret);
        renameColumns((List<AtlasObjectId>) oldTableEntity.getEntity().getAttribute(ATTRIBUTE_PARTITION_KEYS), oldTableEntity, newTableQualifiedName, ret);
        renameStorageDesc((AtlasObjectId) oldTableEntity.getEntity().getAttribute(ATTRIBUTE_STORAGEDESC), oldTableEntity, newTableQualifiedName, ret);

        // update qualifiedName and other attributes (like params - which include lastModifiedTime, lastModifiedBy) of the table
        AtlasEntityWithExtInfo newTableEntity = toTableEntity(newTable);

        // set previous name as the alias
        newTableEntity.getEntity().setAttribute(ATTRIBUTE_ALIASES, Collections.singletonList(oldTable.getTableName()));

        // remove columns, partitionKeys and storageDesc - as they have already been updated above
        removeAttribute(newTableEntity, ATTRIBUTE_COLUMNS);
        removeAttribute(newTableEntity, ATTRIBUTE_PARTITION_KEYS);
        removeAttribute(newTableEntity, ATTRIBUTE_STORAGEDESC);

        AtlasObjectId oldTableId = new AtlasObjectId(oldTableEntity.getEntity().getTypeName(), ATTRIBUTE_QUALIFIED_NAME, oldTableEntity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME));

        ret.add(new EntityPartialUpdateRequestV2(getUserName(), oldTableId, newTableEntity));

        context.removeFromKnownTable((String) oldTableEntity.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME));

        return ret;
    }

    private void renameColumns(List<AtlasObjectId> columns, AtlasEntityExtInfo oldEntityExtInfo, String newTableQualifiedName, List<HookNotification> notifications) {
        if (CollectionUtils.isNotEmpty(columns)) {
            for (AtlasObjectId columnId : columns) {
                AtlasEntity   oldColumn   = oldEntityExtInfo.getEntity(columnId.getGuid());
                AtlasObjectId oldColumnId = new AtlasObjectId(oldColumn.getTypeName(), ATTRIBUTE_QUALIFIED_NAME, oldColumn.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                AtlasEntity   newColumn   = new AtlasEntity(oldColumn.getTypeName(), ATTRIBUTE_QUALIFIED_NAME, getColumnQualifiedName(newTableQualifiedName, (String) oldColumn.getAttribute(ATTRIBUTE_NAME)));

                notifications.add(new EntityPartialUpdateRequestV2(getUserName(), oldColumnId, new AtlasEntityWithExtInfo(newColumn)));
            }
        }
    }

    private void renameStorageDesc(AtlasObjectId sdId, AtlasEntityExtInfo oldEntityExtInfo, String newTableQualifiedName, List<HookNotification> notifications) {
        if (sdId != null) {
            AtlasEntity   oldSd   = oldEntityExtInfo.getEntity(sdId.getGuid());
            AtlasObjectId oldSdId = new AtlasObjectId(oldSd.getTypeName(), ATTRIBUTE_QUALIFIED_NAME, oldSd.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
            AtlasEntity   newSd   = new AtlasEntity(oldSd.getTypeName(), ATTRIBUTE_QUALIFIED_NAME, getStorageDescQualifiedName(newTableQualifiedName));

            notifications.add(new EntityPartialUpdateRequestV2(getUserName(), oldSdId, new AtlasEntityWithExtInfo(newSd)));
        }
    }

    private void removeAttribute(AtlasEntityWithExtInfo entity, String attributeName) {
        Object attributeValue = entity.getEntity().getAttribute(attributeName);

        entity.getEntity().getAttributes().remove(attributeName);

        if (attributeValue instanceof AtlasObjectId) {
            AtlasObjectId objectId = (AtlasObjectId) attributeValue;

            entity.removeReferredEntity(objectId.getGuid());
        } else if (attributeValue instanceof Collection) {
            for (Object item : (Collection) attributeValue)
                if (item instanceof AtlasObjectId) {
                    AtlasObjectId objectId = (AtlasObjectId) item;

                    entity.removeReferredEntity(objectId.getGuid());
                }
        }
    }

    private String getStorageDescQualifiedName(String tblQualifiedName) {
        return tblQualifiedName + "_storage";
    }
}
