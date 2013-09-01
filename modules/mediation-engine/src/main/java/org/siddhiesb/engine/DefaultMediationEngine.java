package org.siddhiesb.engine;

import org.siddhiesb.common.api.*;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;


public class DefaultMediationEngine implements MediationEngineAPI {

    SiddhiManager siddhiManager;
    DefaultSender defaultSender;

    public void init(TransportSenderAPI transportSenderAPI) {

        String executionPlan = "define stream inFlow ( ptcontext string, receivingFlow string, nextFlow string);\n" +
                /*"define stream sender ( ptcontext string, endpoint string, receivingFlow string);\n" +*/
                "from inFlow select ptcontext, 'http://localhost:9000/services/SimpleStockQuoteService' as endpoint, receivingFlow insert into sender;\n";
        defaultSender = new DefaultSender(transportSenderAPI);
        siddhiManager = new SiddhiManager();

        siddhiManager.addExecutionPlan(executionPlan);
        siddhiManager.addCallback(SiddhiESBMediationConstants.SENDER, defaultSender);

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
