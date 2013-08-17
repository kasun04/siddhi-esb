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

package org.apache.synapse.transport.passthru.config;

import java.net.UnknownHostException;

/*import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.axis2.transport.base.threads.WorkerPool;*/
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.connections.SourceConnections;
import org.apache.synapse.transport.passthru.workerpool.WorkerPool;

/**
 * This class stores configurations specific to the Listeners
 */
public class SourceConfiguration extends BaseConfiguration {

    private Log log = LogFactory.getLog(SourceConfiguration.class);

    /** This is used to process HTTP responses */
    private HttpProcessor httpProcessor = null;

    /** Response factory used for creating HTTP Responses */
    private HttpResponseFactory responseFactory = null;

    /** port of the listener */
    private int port = 8280;

    /** Object to manage the source connections */
    private SourceConnections sourceConnections = null;

    private Scheme scheme;
    private String host;

    /** The EPR prefix for services available over this transport */
    private String serviceEPRPrefix;
    /** The EPR prefix for services with custom URI available over this transport */
    private String customEPRPrefix;
    

    public SourceConfiguration(
                               Scheme scheme,
                               WorkerPool workerPool) {
        super(workerPool);
        this.scheme = scheme;
        httpProcessor = new ImmutableHttpProcessor(
                new HttpResponseInterceptor[]{
                        new ResponseDate(),
                        new ResponseServer(),
                        new ResponseContent(),
                        new ResponseConnControl()});

        responseFactory = new DefaultHttpResponseFactory();

        sourceConnections = new SourceConnections();
    }

    public void build() {
        super.build();

    }

    public HttpParams getHttpParams() {
        return httpParams;
    }

    public IOReactorConfig getIOReactorConfig() {
        return ioReactorConfig;
    }

    public HttpProcessor getHttpProcessor() {
        return httpProcessor;
    }

    public HttpResponseFactory getResponseFactory() {
        return responseFactory;
    }


    public SourceConnections getSourceConnections() {
        return sourceConnections;
    }


    public Scheme getScheme() {
        return scheme;
    }

}
