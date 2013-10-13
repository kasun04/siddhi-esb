package org.siddhiesb.engine;

import org.siddhiesb.common.api.*;
import org.siddhiesb.engine.util.XPathEvaluator;
import org.siddhiesb.engine.util.XSLTTransformer;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;


public class DefaultMediationEngine implements MediationEngineAPI {

    SiddhiManager siddhiManager;
    DefaultSender defaultSender;
    DefaultResponder defaultResponder;
    MediationConfigDeployer mediationConfigDeployer;

    public void init(TransportSenderAPI transportSenderAPI) {

        defaultSender = new DefaultSender(transportSenderAPI);
        defaultResponder = new DefaultResponder(transportSenderAPI);

        siddhiManager = new SiddhiManager();

        siddhiManager.addCallback(SiddhiESBMediationConstants.SENDER, defaultSender);
        siddhiManager.addCallback(SiddhiESBMediationConstants.RESPONDER, defaultResponder);

        /*Extension Handling*/
        ArrayList<Class> extensionList = new ArrayList<Class>();
        extensionList.add(XPathEvaluator.class);
        extensionList.add(XSLTTransformer.class);
        siddhiManager.getSiddhiContext().setSiddhiExtensions(extensionList);

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
            /*Use the InFlow as the default API*/
            String api = SiddhiESBMediationConstants.IN_FLOW;
            if (passThruContext.getProperty("To") != null) {
                String urlContext = (String) passThruContext.getProperty("To");
                String[] ctxArray = urlContext.split("/");
                if (ctxArray.length > 1) {
                    api = ctxArray[1];
                }
            }

            InputHandler inputHandler = siddhiManager.getInputHandler(api);

            /*If the API flow cannot be found, then route to default inFlow*/
            if (inputHandler == null) {
                inputHandler = siddhiManager.getInputHandler(SiddhiESBMediationConstants.IN_FLOW);
            }
            inputHandler.send(new Object[]{passThruContext, "recFlow", "nxtFlow"});

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

       /* passThruContext.setProperty(CommonAPIConstants.ENDPOINT, "http://localhost:9000/service/EchoService");
        defaultSender.send(passThruContext);*/
    }

    private void processResponse(PassThruContext passThruContext) {

        try {
            InputHandler inputHandler = siddhiManager.getInputHandler(SiddhiESBMediationConstants.OUT_FLOW);
            inputHandler.send(new Object[]{passThruContext, "recFlow", "nxtFlow"});
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //defaultSender.send(passThruContext);

    }


}
