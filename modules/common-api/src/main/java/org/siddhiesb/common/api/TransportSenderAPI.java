package org.siddhiesb.common.api;


public interface TransportSenderAPI {

    public void init(MediationEngineAPI mediationEngine);

    public void invoke(PassThruContext passThruContext);

    public void destroy();
}
