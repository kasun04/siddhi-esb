package org.apache.synapse.common.api;


public interface PassThruContext {

    public void init();

    public Object getProperty(String key);

    public void setProperty(String key, Object val);

    public void reset();

}
