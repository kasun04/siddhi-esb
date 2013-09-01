package org.siddhiesb.engine;

import org.siddhiesb.common.api.CommonAPIConstants;
import org.siddhiesb.common.api.PassThruContext;
import org.wso2.siddhi.core.event.Event;

public class MediationEngineUtils {

    public static PassThruContext extractPTContext(Event[] events) {
        PassThruContext passThruContext = null;
        if (events != null &&
                events[0] != null && events[0].getData0() instanceof PassThruContext) {
            passThruContext = (PassThruContext) events[0].getData0();
        }

        if (passThruContext != null) {
            passThruContext.setProperty(CommonAPIConstants.ENDPOINT, events[0].getData1());
        }

        return passThruContext;
    }

}
