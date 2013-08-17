/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.transport.passthru.util.builders;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.Handler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.InputStream;

public class RelayUtils {

   	private static final Log log = LogFactory.getLog(RelayUtils.class);
	  

    private static volatile Handler addressingInHandler = null;
    private static boolean noAddressingHandler = false;
    
    private static Boolean forcePTBuild = null;
    

    static{
    	if(forcePTBuild == null){
           forcePTBuild =PassThroughConfiguration.getInstance().getBooleanProperty(PassThroughConstants.FORCE_PASSTHROUGH_BUILDER);
           if(forcePTBuild ==null){
             forcePTBuild =true;
           }
        //this to keep track ignore the builder operation eventhough content level is enable.
        }
    }

	public static void buildMessage(org.apache.axis2.context.MessageContext msgCtx) throws IOException,
            XMLStreamException {

        buildMessage(msgCtx, false);
    }

    public static void buildMessage(MessageContext messageContext, boolean earlyBuild) throws IOException,
            XMLStreamException {

        final Pipe pipe = (Pipe) messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
		if (pipe != null &&
		    !Boolean.TRUE.equals(messageContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)) &&
		    forcePTBuild) {
			InputStream in = pipe.getInputStream();
        	
        	builldMessage(messageContext, earlyBuild, in);
            return;
        }
    }

	public static void builldMessage(MessageContext messageContext, boolean earlyBuild, InputStream in) throws IOException, AxisFault {
	    return;
    }

    private static Boolean getDisableAck(MessageContext msgContext) throws AxisFault {
       // We should send an early ack to the transport whenever possible, but some modules need
       // to use the back channel, so we need to check if they have disabled this code.
       Boolean disableAck = (Boolean) msgContext.getProperty(Constants.Configuration.DISABLE_RESPONSE_ACK);
       if(disableAck == null) {
          disableAck = (Boolean) (msgContext.getAxisService() != null ? msgContext.getAxisService().getParameterValue(Constants.Configuration.DISABLE_RESPONSE_ACK) : null);
       }

       return disableAck;
    }

    private static boolean isOneWay(String mepString) {
        return (mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY)
                || mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY)
                || mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY));
    }
}
