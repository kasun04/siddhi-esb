package org.siddhiesb.engine;


import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.common.api.MessageSenderAPI;
import org.siddhiesb.common.api.TransportSenderAPI;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.output.StreamCallback;

public class DefaultMessageSender extends StreamCallback implements MessageSenderAPI {
    private TransportSenderAPI transportSender;

    public DefaultMessageSender(TransportSenderAPI transportSender) {
        this.transportSender = transportSender;
    }

    /* Receive callback event from Siddhi Engine */
    public void receive(Event[] events) {
        //EventPrinter.print(events);
        CommonContext reqCommonContext = MediationEngineUtils.extractPTContext(events);

        if (reqCommonContext != null) {
            send(reqCommonContext);
        }
    }

    public void send(CommonContext commonContext) {
        transportSender.invoke(commonContext);
    }

}
