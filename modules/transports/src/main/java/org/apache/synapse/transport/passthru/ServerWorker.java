/*
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

import java.io.OutputStream;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.common.api.DefaultPassThruContext;
import org.apache.synapse.common.api.MediationEngineAPI;
import org.apache.synapse.common.api.PassThruContext;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;

/**
 * This is a worker thread for executing an incoming request in to the transport.
 */
public class ServerWorker implements Runnable {

    private static final Log log = LogFactory.getLog(ServerWorker.class);
    /**
     * the http request
     */
    private SourceRequest request = null;
    /**
     * The configuration of the receiver
     */
    private SourceConfiguration sourceConfiguration = null;

    private static final String SOAP_ACTION_HEADER = "SOAPAction";

    /**
     * Weather we should do rest dispatching or not
     */
    private boolean isRestDispatching = true;


    /*Generic Mediation Engine*/
    private MediationEngineAPI genericMediationEngine;


    private PassThruContext passThruContext;

    public ServerWorker(final SourceRequest request,
                        final SourceConfiguration sourceConfiguration,
                        final OutputStream os,
                        MediationEngineAPI mediationEngine) {
        this.request = request;
        this.sourceConfiguration = sourceConfiguration;

        passThruContext = new DefaultPassThruContext();
        passThruContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_REQUEST, request);
        passThruContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION, sourceConfiguration);
        passThruContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION, request.getConnection());

        genericMediationEngine = mediationEngine;
    }

    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Starting a new Server Worker instance");
        }

        String method = request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase() : "";

        /*service dispatching logic removed... */

        if ("GET".equals(method) || "DELETE".equals(method) || "OPTIONS".equals(method) || "HEAD".equals(method)) {

            HttpResponse response = sourceConfiguration.getResponseFactory().newHttpResponse(
                    request.getVersion(), HttpStatus.SC_OK,
                    request.getConnection().getContext());

            // create a basic HttpEntity using the source channel of the response pipe
            BasicHttpEntity entity = new BasicHttpEntity();
            if (request.getVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
                entity.setChunked(true);
            }
            response.setEntity(entity);
        }

        if (request.isEntityEnclosing()) {
            processEntityEnclosingRequest();
        } else {
            processNonEntityEnclosingRESTHandler(null);
        }
        sendAck();
    }

    private void sendAck() {

    }

    private void processNonEntityEnclosingRESTHandler(SOAPEnvelope soapEnvelope) {
        String soapAction = request.getHeaders().get(SOAP_ACTION_HEADER);
        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }
        /*
        ToDo : Dispatch to Engine
        AxisEngine.receive(msgContext);
       */
    }

    private void processEntityEnclosingRequest() {
        String contentTypeHeader = request.getHeaders().get(HTTP.CONTENT_TYPE);
        String method = request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase() : "";

        passThruContext.setProperty("To", request.getUri());
        passThruContext.setProperty(HTTPConstants.HTTP_METHOD, method);
        passThruContext.setProperty(HTTP.CONTENT_TYPE, contentTypeHeader);

        /*Setting the PassThru Pipe*/
        passThruContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, request.getPipe());

        /*ToDo : Set Pipe and dispatch to Mediation Engine */
        genericMediationEngine.process(passThruContext);



    }


    private boolean isRest(String contentType) {
        return contentType != null &&
                contentType.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) == -1 &&
                contentType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) == -1;
    }

    private void handleException(String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (e == null) {
            e = new Exception(msg);
        }
    }

}
