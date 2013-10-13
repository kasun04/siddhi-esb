package org.siddhiesb.engine;

import org.siddhiesb.common.api.CommonAPIConstants;
import org.siddhiesb.common.api.PassThruContext;
import org.wso2.siddhi.core.event.Event;

public class MediationEngineUtils {

    public static PassThruContext extractPTContext(Event[] events) {
        PassThruContext passThruContext = null;
        if (events != null && events.length > 0 && events[0] != null) {
            Object[] contextObjects = events[0].getData();
            if (contextObjects.length >= 2) {
                passThruContext = (PassThruContext)contextObjects[0];
                passThruContext.setProperty(CommonAPIConstants.ENDPOINT, contextObjects[1]);
            } else if (contextObjects.length == 1) {
                passThruContext  = (PassThruContext)contextObjects[0];
            }
        }
        return passThruContext;
    }

}
