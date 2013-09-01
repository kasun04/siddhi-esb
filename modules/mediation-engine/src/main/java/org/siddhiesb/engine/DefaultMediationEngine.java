package org.siddhiesb.engine;

import org.siddhiesb.common.api.*;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.io.FileReader;
import java.util.Scanner;


public class DefaultMediationEngine implements MediationEngineAPI {

    SiddhiManager siddhiManager;
    DefaultSender defaultSender;
    MediationConfigDeployer mediationConfigDeployer;

    public void init(TransportSenderAPI transportSenderAPI) {

        defaultSender = new DefaultSender(transportSenderAPI);
        siddhiManager = new SiddhiManager();

        siddhiManager.addCallback(SiddhiESBMediationConstants.SENDER, defaultSender);

        /*Mediation Config Deployer*/
        mediationConfigDeployer = new MediationConfigDeployer();
        mediationConfigDeployer.init();
        mediationConfigDeployer.setSiddhiManager(siddhiManager);

        /*initial cold deployment*/
        mediationConfigDeployer.deploy();

    }

    public void process(PassThruContext passThruContext) {

        if (CommonAPIConstants.MESSAGE_DIRECTION_REQUEST.
                equals(passThruContext.getProperty(CommonAPIConstants.MESSAGE_DIRECTION))) {
            /*Handling Request*/
            processRequest(passThruContext);
        } else if (CommonAPIConstants.MESSAGE_DIRECTION_RESPONSE.
                equals(passThruContext.getProperty(CommonAPIConstants.MESSAGE_DIRECTION))) {
            /*Handling Response*/
            processResponse(passThruContext);
        }
    }

    public void stop() {

    }

    private void processRequest(PassThruContext passThruContext) {
        try {
            InputHandler inputHandler = siddhiManager.getInputHandler(SiddhiESBMediationConstants.IN_FLOW);
            inputHandler.send(new Object[]{passThruContext, "recFlow", "nxtFlow"});
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void processResponse(PassThruContext passThruContext) {
        defaultSender.send(passThruContext);

    }


}