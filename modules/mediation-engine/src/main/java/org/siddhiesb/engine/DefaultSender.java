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
        //EventPrinter.print(events);
        PassThruContext reqPassThruContext = MediationEngineUtils.extractPTContext(events);

        if (reqPassThruContext != null) {
            send(reqPassThruContext);
        }
    }

    public void send(PassThruContext passThruContext) {
        transportSender.invoke(passThruContext);
    }

}
