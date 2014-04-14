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

package org.siddhiesb.transport.passthru;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.siddhiesb.common.api.CommonAPIConstants;
import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.common.api.DefaultCommonContext;
import org.siddhiesb.common.api.MediationEngineAPI;

public class ClientWorker implements Runnable {
    private Log log = LogFactory.getLog(ClientWorker.class);
    /** the response message context that would be created */
    /** the HttpResponse received */
    private TargetResponse response = null;
    /** weather a body is expected or not */
    private boolean expectEntityBody = true;

    private CommonContext clientWorkerPTCtx;

    private MediationEngineAPI genericMediationEngine;



    public ClientWorker(CommonContext commonContext,
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

        clientWorkerPTCtx = new DefaultCommonContext();

        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION,
                commonContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION));
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION,
                commonContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION));

        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, response.getPipe());
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_RESPONSE, response);
        clientWorkerPTCtx.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_CONNECTION, response.getConnection());

        /*To identify a response in the transport sender*/
        clientWorkerPTCtx.setProperty(CommonAPIConstants.MESSAGE_DIRECTION, CommonAPIConstants.MESSAGE_DIRECTION_RESPONSE);
        /*Co-relation Request-Response*/
        clientWorkerPTCtx.setCtxId(commonContext.getCtxId());

        clientWorkerPTCtx.setProperty(PassThroughConstants.HTTP_HEADERS, response.getHeaders());

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

        /*Dispatching to the MediationEngine*/
        genericMediationEngine.process(clientWorkerPTCtx);

    }

    private String inferContentType() {
        // Try to get the content type from the message context
        return PassThroughConstants.DEFAULT_CONTENT_TYPE;
    }

}
