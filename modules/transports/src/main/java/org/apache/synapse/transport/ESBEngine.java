package org.apache.synapse.transport;

import org.apache.synapse.common.api.MediationEngineAPI;
import org.apache.synapse.common.api.PassThruContext;
import org.apache.synapse.engine.DefaultMediationEngine;
import org.apache.synapse.transport.passthru.TransportListener;
import org.apache.synapse.transport.passthru.TransportSender;

public class ESBEngine {

    private TransportSender transportSender;

    public ESBEngine() {
        transportSender = new TransportSender();
        //transportSender.init();
    }

    public void process(PassThruContext passThruContext) {
        System.out.println("-------------- Internal Mediation Engine ---------------- processed!");
        transportSender.invoke(passThruContext);
    }


    public static void main(String[] args) {
        TransportListener transportListener = new TransportListener();
        MediationEngineAPI mediationEngine = new DefaultMediationEngine();

        transportListener.init(mediationEngine);
        transportListener.start();


    }
}
