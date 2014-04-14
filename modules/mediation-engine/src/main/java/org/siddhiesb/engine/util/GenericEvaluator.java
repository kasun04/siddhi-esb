package org.siddhiesb.engine.util;

import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;


@SiddhiExtension(namespace = "siddhiESB", function = "eval")

public class GenericEvaluator  extends FunctionExecutor {

    public void init(Attribute.Type[] types, SiddhiContext siddhiContext) {

    }


    public void destroy(){

    }


    protected Object process(Object eventObj) {
        return null;
    }

    public Object execute(AtomicEvent event) {

        return process(event);
    }

    public Attribute.Type getReturnType() {
        return Attribute.Type.STRING;
    }
}
