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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.siddhiesb.transport.passthru.*;
import org.siddhiesb.transport.passthru.ProtocolState;
import org.siddhiesb.transport.passthru.TargetContext;
import org.siddhiesb.common.api.PassThruContext;
import org.siddhiesb.transport.passthru.config.TargetConfiguration;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a class for representing a request to be sent to a target.
 */
public class TargetRequest {
    /** Configuration of the sender */
    private TargetConfiguration targetConfiguration;
    private HttpRoute route;
    private org.siddhiesb.transport.passthru.Pipe pipe = null;
    /** Headers map */
    private Map<String, String> headers = new HashMap<String, String>();
    /** URL */
    private URL url;
    /** HTTP Method */
    private String method;
    /** HTTP request created for sending the message */
    private HttpRequest request = null;
    /** Weather chunk encoding should be used */
    private boolean chunk = true;
    /** HTTP version that should be used */
    private ProtocolVersion version = null;
    /** Weather full url is used for the request */
    private boolean fullUrl = false;
    /** Port to be used for the request */
    private int port = 80;
    /** Weather this request has a body */
    private boolean hasEntityBody = true;
    /** Keep alive request */
    private boolean keepAlive = true;

    private PassThruContext passThruContext;
    
    /**
     * Create a target request.
     *
     * @param targetConfiguration the configuration of the sender
     * @param url the url to be used
     * @param method the HTTP method
     * @param hasEntityBody weather request has an entity body
     */
    public TargetRequest(TargetConfiguration targetConfiguration, HttpRoute route, URL url,
                         String method, boolean hasEntityBody) {
        this(targetConfiguration, route, method, url, hasEntityBody);
    }

    public TargetRequest(TargetConfiguration targetConfiguration, HttpRoute route, String method,
                         URL url, boolean hasEntityBody) {
        this.route = route;
        this.method = method;
        this.url = url;
        this.targetConfiguration = targetConfiguration;
        this.hasEntityBody = hasEntityBody;
    }

    public void connect(org.siddhiesb.transport.passthru.Pipe pipe) {
        this.pipe = pipe;
    }

    public void start(NHttpClientConnection conn) throws IOException, HttpException {
        if (pipe != null) {
            org.siddhiesb.transport.passthru.TargetContext.get(conn).setWriter(pipe);
        }

        String path = fullUrl || (route.getProxyHost() != null && !route.isTunnelled()) ?
                    url.toString() : url.getPath() +
                    (url.getQuery() != null ? "?" + url.getQuery() : "");

        long contentLength = -1;
        String contentLengthHeader = headers.get(HTTP.CONTENT_LEN);
        if (contentLengthHeader != null) {
            contentLength = Integer.parseInt(contentLengthHeader);
            headers.remove(HTTP.CONTENT_LEN);
        }
        
        passThruContext = org.siddhiesb.transport.passthru.TargetContext.get(conn).getPassThruContext();

        if (hasEntityBody) {
            request = new BasicHttpEntityEnclosingRequest(method, path,
                    version != null ? version : HttpVersion.HTTP_1_1);

            BasicHttpEntity entity = new BasicHttpEntity();

            ((BasicHttpEntityEnclosingRequest) request).setEntity(entity);
           
        } else {
            request = new BasicHttpRequest(method, path,
                    version != null ? version : HttpVersion.HTTP_1_1);
        }

        Set<Map.Entry<String, String>> entries = headers.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            request.setHeader(entry.getKey(), entry.getValue());
        }


        request.setParams(new DefaultedHttpParams(request.getParams(),
                targetConfiguration.getHttpParams()));
        
		this.processChunking(conn, passThruContext);
		

        if (!keepAlive) {
            request.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
        }
        
       

        // Pre-process HTTP request
        conn.getContext().setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        conn.getContext().setAttribute(ExecutionContext.HTTP_TARGET_HOST,
                new HttpHost(url.getHost(), port));
        conn.getContext().setAttribute(ExecutionContext.HTTP_REQUEST, request);

        // start the request
        targetConfiguration.getHttpProcessor().process(request, conn.getContext());
        
        if (targetConfiguration.getProxyAuthenticator() != null 
                && route.getProxyHost() != null && !route.isTunnelled()) {
            targetConfiguration.getProxyAuthenticator().authenticatePreemptively(request, conn.getContext());
        }
        
        conn.submitRequest(request);

        if (hasEntityBody) {
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD);
        } else {
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE);
        }
    }
    

	private void processChunking(NHttpClientConnection conn, PassThruContext requestMsgCtx){

    }

    /**
     * Consume the data from the pipe and write it to the wire.
     *
     * @param conn the connection to the target
     * @param encoder encoder for writing the message through
     * @throws java.io.IOException if an error occurs
     * @return number of bytes written
     */
    public int write(NHttpClientConnection conn, ContentEncoder encoder) throws IOException {
        int bytes = 0;
        if (pipe != null) {
            bytes = pipe.consume(encoder);
        }

        if (encoder.isCompleted()) {
            TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
        }
        
        return bytes;

    }

    public boolean hasEntityBody() {
        return hasEntityBody;
    }
    
    
    public void setHasEntityBody(boolean hasEntityBody) {
		this.hasEntityBody = hasEntityBody;
	}

	public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getMethod() {
        return method;
    }

    public void setChunk(boolean chunk) {
        this.chunk = chunk;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setFullUrl(boolean fullUrl) {
        this.fullUrl = fullUrl;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

	public HttpRequest getRequest() {
		return request;
	}
    
    
}
