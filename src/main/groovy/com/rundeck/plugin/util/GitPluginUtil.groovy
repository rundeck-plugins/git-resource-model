package com.rundeck.plugin.util

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.plugin.GitFailureReason


/**
 * Created by luistoledo on 12/18/17.
 */
class GitPluginUtil {
    static Map<String, Object> getRenderOpt(String value, boolean secondary, boolean password = false, boolean storagePassword = false) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(StringRenderingConstants.GROUP_NAME,value);
        if(secondary){
            ret.put(StringRenderingConstants.GROUPING,"secondary");
        }
        if(password){
            ret.put("displayType",StringRenderingConstants.DisplayType.PASSWORD)
        }
        if(storagePassword){
            ret.put(StringRenderingConstants.SELECTION_ACCESSOR_KEY,StringRenderingConstants.SelectionAccessor.STORAGE_PATH)
            ret.put(StringRenderingConstants.STORAGE_PATH_ROOT_KEY,"keys")
            ret.put(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
        }

        return ret;
    }

    static String getPasswordFromKeyStorage(String path, StorageTree storage) {
        try{
            ResourceMeta contents = storage.getResource(path).getContents()
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
            contents.writeContent(byteArrayOutputStream)
            String password = new String(byteArrayOutputStream.toByteArray())

            return password
        }catch(Exception e){
            throw new StepException("error accessing ${path}: ${e.message}", GitFailureReason.KeyStorage)
        }

    }

    static String getSshKeyFromKeyStorage(String path, StorageTree storage) {
        try{
            ResourceMeta contents = storage.getResource(path).getContents()
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
            contents.writeContent(byteArrayOutputStream)
            return byteArrayOutputStream.toByteArray()
        }catch(Exception e){
            throw new StepException("error accessing ${path}: ${e.message}", GitFailureReason.KeyStorage)
        }

    }

}
