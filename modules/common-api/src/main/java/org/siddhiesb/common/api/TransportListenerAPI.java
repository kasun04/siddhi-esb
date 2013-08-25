package org.siddhiesb.common.api;


public interface TransportListenerAPI {

    public void init(MediationEngineAPI mediationEngineAPI);

    public void start();

    public void stop();
}
