/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.common.api.DefaultPassThruContext;
import org.apache.synapse.common.api.MediationEngineAPI;
import org.apache.synapse.common.api.PassThruContext;
import org.apache.synapse.transport.ESBEngine;

public class ClientWorker implements Runnable {
    private Log log = LogFactory.getLog(ClientWorker.class);
    /** the response message context that would be created */
    /** the HttpResponse received */
    private TargetResponse response = null;
    /** weather a body is expected or not */
    private boolean expectEntityBody = true;

    private PassThruContext clientWorkerPTCtx;

    private MediationEngineAPI genericMediationEngine;



    public ClientWorker(PassThruContext passThruContext,
                        TargetResponse response,
                        MediationEngineAPI mediationEngine) {
        this.response = response;
        this.expectEntityBody = response.isExpectResponseBody();

        Map<String,String> headers = response.getHeaders();
        Map excessHeaders = response.getExcessHeaders();
      
		String oriURL = headers.get(PassThroughConstants.LOCATION);
		
		// Special casing 302 & 301 scenario in following section. Not sure whether it's the correct fix,
		// but this fix makes it possible to do http --> https redirection.
        if (oriURL != null && ((response.getStatus() != HttpStatus.SC_MOVED_TEMPORARILY) &&
                response.getStatus() != HttpStatus.SC_MOVED_PERMANENTLY)) {
            URL url;
            try {
                url = new URL(oriURL);
            } catch (MalformedURLException e) {
                log.error("Invalid URL received", e);
                return;
            }

            headers.remove(PassThroughConstants.LOCATION);

        }

        clientWorkerPTCtx = new DefaultPassThruContext();

        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION,
                passThruContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION));
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION,
                passThruContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION));

        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, response.getPipe());
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_RESPONSE, response);
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_CONNECTION, response.getConnection());

        /*To identify a response in the transport sender*/
        clientWorkerPTCtx.setProperty("RESPONSE", "TRUE");

        genericMediationEngine = mediationEngine;
    }

    public void run() {

        if (expectEntityBody) {
            String cType = response.getHeader(HTTP.CONTENT_TYPE);
            String contentType;
            if (cType != null) {
                // This is the most common case - Most of the time servers send the Content-Type
                contentType = cType;
            } else {
                // Server hasn't sent the header - Try to infer the content type
                contentType = inferContentType();
            }
        }
        // copy the HTTP status code as a message context property with the key HTTP_SC to be
        // used at the sender to set the proper status code when passing the message
        int statusCode = this.response.getStatus();
        // process response received
        System.out.println("Client Worker Invoked .... ");
        /*ToDo Dispatch to Engine*/
        genericMediationEngine.process(clientWorkerPTCtx);
        //new ESBEngine().process(clientWorkerPTCtx);
    }

    private String inferContentType() {
        // Try to get the content type from the message context
        return PassThroughConstants.DEFAULT_CONTENT_TYPE;
    }

}
