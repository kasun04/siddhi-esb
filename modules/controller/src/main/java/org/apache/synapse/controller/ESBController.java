package org.apache.synapse.controller;


import org.apache.synapse.common.api.MediationEngineAPI;
import org.apache.synapse.common.api.TransportListenerAPI;
import org.apache.synapse.common.api.TransportSenderAPI;
import org.apache.synapse.engine.DefaultMediationEngine;
import org.apache.synapse.transport.passthru.TransportListener;
import org.apache.synapse.transport.passthru.TransportSender;

public class ESBController {

    private TransportListenerAPI transportListener;
    private MediationEngineAPI mediationEngine;

    private TransportSenderAPI transportSender;

    public void start() {

        transportListener = new TransportListener();
        transportSender = new TransportSender();

        mediationEngine = new DefaultMediationEngine();
        mediationEngine.init(transportSender);


        /*Mediation Engine for request path*/
        transportListener.init(mediationEngine);

        /*Mediation Engine For Response Path*/
        transportSender.init(mediationEngine);

        /*Starting TransportListener*/
        transportListener.start();


    }

    public static void main(String[] args) {
        new ESBController().start();
    }


}
