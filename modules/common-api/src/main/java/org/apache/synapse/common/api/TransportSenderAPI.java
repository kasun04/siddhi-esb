package org.apache.synapse.common.api;


public interface TransportSenderAPI {

    public void init(MediationEngineAPI mediationEngine);

    public void invoke(PassThruContext passThruContext);
}
