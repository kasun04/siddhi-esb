package org.siddhiesb.common.api;


import java.util.HashMap;
import java.util.Map;

public class DefaultPassThruContext implements PassThruContext {

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

    public void reset() {
        propertyMap.clear();
    }
}
