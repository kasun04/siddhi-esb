package org.siddhiesb.controller;


import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.TransportListenerAPI;
import org.siddhiesb.common.api.TransportSenderAPI;
import org.siddhiesb.engine.DefaultMediationEngine;
import org.siddhiesb.transport.passthru.TransportListener;
import org.siddhiesb.transport.passthru.TransportSender;

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
