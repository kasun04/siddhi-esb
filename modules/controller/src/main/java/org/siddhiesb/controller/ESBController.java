package org.siddhiesb.controller;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.TransportListenerAPI;
import org.siddhiesb.common.api.TransportSenderAPI;
import org.siddhiesb.engine.DefaultMediationEngine;
import org.siddhiesb.transport.passthru.TransportListener;
import org.siddhiesb.transport.passthru.TransportSender;

public class ESBController {

    Log log;
    private TransportListenerAPI transportListener;
    private MediationEngineAPI mediationEngine;

    private TransportSenderAPI transportSender;

    public void start() {
        this.log = LogFactory.getLog(this.getClass());
        log.info("Starting Controller =============");

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

    public void stop() {
        transportListener.stop();
        mediationEngine.stop();
        transportSender.destroy();
    }

    public static void main(String[] args) {

        new ESBController().start();
    }


}
