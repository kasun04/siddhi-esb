package org.siddhiesb.common.api;


public interface CommonContext {

    public void init();

    public Object getProperty(String key);

    public void setProperty(String key, Object val);

    public void reset();

    public void setCtxId(String messageId);

    public String getCtxId();



}
