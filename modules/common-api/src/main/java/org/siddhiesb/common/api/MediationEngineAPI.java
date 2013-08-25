package org.siddhiesb.common.api;


public interface MediationEngineAPI {

    public void init(TransportSenderAPI transportSender);

    public void process(PassThruContext passThruContext);

    public void stop();



}
