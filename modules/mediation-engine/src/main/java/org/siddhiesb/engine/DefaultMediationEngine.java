package org.siddhiesb.engine;

import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.PassThruContext;
import org.siddhiesb.common.api.TransportSenderAPI;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.query.Query;
import org.wso2.siddhi.query.compiler.SiddhiCompiler;


public class DefaultMediationEngine implements MediationEngineAPI {

    private TransportSenderAPI transportSender;
    SiddhiManager siddhiManager;
    InputHandler inputHandler;
    StreamDefinition streamDefinition;
    String queryReference;
    PassThruContext ctx;


    public void init(TransportSenderAPI transportSenderAPI) {
        transportSender = transportSenderAPI;

        siddhiManager = new SiddhiManager();

        streamDefinition = SiddhiCompiler.parseStreamDefinition("define stream cseEventStream ( symbol string, price float, volume int )");
        inputHandler = siddhiManager.defineStream(streamDefinition);
        Query query = SiddhiCompiler.parseQuery("from  cseEventStream [price >= 20] " +
                "select symbol, avg(price) as avgPrice " +
                "group by symbol " +
                "having avgPrice>50 " +
                "insert into StockQuote; "
        );
        queryReference = siddhiManager.addQuery(query);
        siddhiManager.addCallback(queryReference, new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                System.out.println("Called.........");
                transportSender.invoke(ctx);
            }
        });
    }

    public void process(PassThruContext passThruContext) {
        //transportSender.invoke(passThruContext);
        ctx = passThruContext;
        try {
            inputHandler.send(new Object[]{"IBM", 75.6f, 100});
        } catch (InterruptedException e) {
        }


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


}
