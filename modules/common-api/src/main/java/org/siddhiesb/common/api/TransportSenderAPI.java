package org.siddhiesb.common.api;


public interface TransportSenderAPI {

    public void init(MediationEngineAPI mediationEngine);

    public void invoke(CommonContext commonContext);

    public void destroy();
}
