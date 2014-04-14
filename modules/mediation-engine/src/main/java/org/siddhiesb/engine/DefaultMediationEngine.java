package org.siddhiesb.engine;

import org.siddhiesb.common.api.*;
import org.siddhiesb.engine.util.XPathEvaluator;
import org.siddhiesb.engine.util.XSLTTransformer;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.util.ArrayList;


public class DefaultMediationEngine implements MediationEngineAPI {

    SiddhiManager siddhiManager;
    DefaultMessageSender defaultSender;
    DefaultResponder defaultResponder;
    MediationConfigDeployer mediationConfigDeployer;

    public void init(TransportSenderAPI transportSenderAPI) {

        defaultSender = new DefaultMessageSender(transportSenderAPI);
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

    public void process(CommonContext commonContext) {

        if (CommonAPIConstants.MESSAGE_DIRECTION_REQUEST.
                equals(commonContext.getProperty(CommonAPIConstants.MESSAGE_DIRECTION))) {
            /*Handling Request*/
            processRequest(commonContext);
        } else if (CommonAPIConstants.MESSAGE_DIRECTION_RESPONSE.
                equals(commonContext.getProperty(CommonAPIConstants.MESSAGE_DIRECTION))) {
            /*Handling Response*/
            processResponse(commonContext);
        }
    }

    public void stop() {

    }

    private void processRequest(CommonContext commonContext) {
        try {
            /*Use the InFlow as the default API*/
            String api = SiddhiESBMediationConstants.IN_FLOW;
            if (commonContext.getProperty("To") != null) {
                String urlContext = (String) commonContext.getProperty("To");
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
            inputHandler.send(new Object[]{commonContext, "recFlow", "nxtFlow"});

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

       /* commonContext.setProperty(CommonAPIConstants.ENDPOINT, "http://localhost:9000/service/EchoService");
        defaultSender.send(commonContext);*/
    }

    private void processResponse(CommonContext commonContext) {

        try {
            InputHandler inputHandler = siddhiManager.getInputHandler(SiddhiESBMediationConstants.OUT_FLOW);
            inputHandler.send(new Object[]{commonContext, "recFlow", "nxtFlow"});
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //defaultSender.send(commonContext);

    }


}
