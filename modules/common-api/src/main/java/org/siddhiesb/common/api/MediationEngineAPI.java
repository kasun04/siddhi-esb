package org.siddhiesb.common.api;


public interface MediationEngineAPI {

    public void init(TransportSenderAPI transportSender);

    public void process(CommonContext commonContext);

    public void stop();



}
