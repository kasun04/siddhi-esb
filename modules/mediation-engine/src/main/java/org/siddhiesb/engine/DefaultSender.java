package org.siddhiesb.engine;


import org.siddhiesb.common.api.DefaultPassThruContext;
import org.siddhiesb.common.api.PassThruContext;
import org.siddhiesb.common.api.SenderAPI;
import org.siddhiesb.common.api.TransportSenderAPI;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

public class DefaultSender extends StreamCallback implements SenderAPI {
    private TransportSenderAPI transportSender;

    public DefaultSender(TransportSenderAPI transportSender) {
        this.transportSender = transportSender;
    }

    /* Receive callback event from Siddhi Engine */
    public void receive(Event[] events) {
        EventPrinter.print(events);
        PassThruContext reqPassThruContext = MediationEngineUtils.extractPTContext(events);
        if (reqPassThruContext != null) {
            send(reqPassThruContext);
        }
    }

    public void send(PassThruContext passThruContext) {
        transportSender.invoke(passThruContext);
    }

    public static void main(String[] args) {
        /*String executionPlan = "define stream inFlow ( ptcontext string, receivingFlow string, nextFlow string);\n" +
                "define stream sender ( ptcontext string, endpoint string, receivingFlow string);\n" +
                "\n" +
                "from inFlow select ptcontext, 'http://localhost:9000/services/SimpleStockQuoteService' as endpoint, receivingFlow insert into sender;";
        SiddhiManager siddhiManager;

        siddhiManager = new SiddhiManager();
        siddhiManager.addExecutionPlan(executionPlan);
        siddhiManager.addCallback("sender", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
            }
        });

        InputHandler inputHandler;

        inputHandler = siddhiManager.getInputHandler("inFlow");
        try {
            inputHandler.send(new Object[]{new DefaultPassThruContext(), "recFlow", "nxtFlow"});
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
    }

}
