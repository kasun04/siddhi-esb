package org.siddhiesb.common.api;


import java.util.HashMap;
import java.util.Map;

public class DefaultCommonContext implements CommonContext {

    private String messageId;

    private Map<String, Object> propertyMap = new HashMap<String, Object>();


    public void init() {
        /*Add default properties */
    }

    public Object getProperty(String key) {

        return propertyMap.get(key);
    }

    public void setProperty(String key, Object val) {
        propertyMap.put(key, val);
    }

    public String getCtxId() {
        return messageId;
    }

    public void setCtxId(String messageId) {
        this.messageId = messageId;
    }

    public void reset() {
        propertyMap.clear();
    }
}
