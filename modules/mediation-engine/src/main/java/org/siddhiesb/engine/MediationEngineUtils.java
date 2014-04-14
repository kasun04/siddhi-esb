package org.siddhiesb.engine;

import org.siddhiesb.common.api.CommonAPIConstants;
import org.siddhiesb.common.api.CommonContext;
import org.wso2.siddhi.core.event.Event;

public class MediationEngineUtils {

    public static CommonContext extractPTContext(Event[] events) {
        CommonContext commonContext = null;
        if (events != null && events.length > 0 && events[0] != null) {
            Object[] contextObjects = events[0].getData();
            if (contextObjects.length >= 2) {
                commonContext = (CommonContext)contextObjects[0];
                commonContext.setProperty(CommonAPIConstants.ENDPOINT, contextObjects[1]);
            } else if (contextObjects.length == 1) {
                commonContext = (CommonContext)contextObjects[0];
            }
        }
        return commonContext;
    }

}
