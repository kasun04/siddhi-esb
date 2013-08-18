package org.apache.synapse.engine;

import org.apache.synapse.common.api.*;


public class DefaultMediationEngine implements MediationEngineAPI {

    private TransportSenderAPI transportSender;


    public void init(TransportSenderAPI transportSenderAPI) {
        transportSender = transportSenderAPI;
    }

    public void process(PassThruContext passThruContext) {
        //System.out.println("=== Injected to Default Mediation Engine ===");
        transportSender.invoke(passThruContext);

    }
}
