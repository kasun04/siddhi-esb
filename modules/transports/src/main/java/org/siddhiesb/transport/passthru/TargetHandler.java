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

package org.siddhiesb.transport.passthru;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.protocol.HttpContext;
import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.transport.http.conn.ClientConnFactory;
import org.siddhiesb.transport.http.conn.ProxyTunnelHandler;
import org.siddhiesb.transport.passthru.config.TargetConfiguration;
import org.siddhiesb.transport.passthru.connections.HostConnections;

/**
 * This class is handling events from the transport -- > client.
 */
public class TargetHandler implements NHttpClientEventHandler {
    private static Log log = LogFactory.getLog(TargetHandler.class);

    /** Delivery agent */
    private final org.siddhiesb.transport.passthru.DeliveryAgent deliveryAgent;

    /** Connection factory */
    private final ClientConnFactory connFactory;
    
    /** Configuration used by the sender */
    private final TargetConfiguration targetConfiguration;


    private MediationEngineAPI mediationEngine;

    public TargetHandler(org.siddhiesb.transport.passthru.DeliveryAgent deliveryAgent,
                         ClientConnFactory connFactory,
                         TargetConfiguration configuration, MediationEngineAPI mediationEngineAPI) {
        this.deliveryAgent = deliveryAgent;
        this.connFactory = connFactory;
        this.targetConfiguration = configuration;

        mediationEngine = mediationEngineAPI;

    }

    public void connected(NHttpClientConnection conn, Object o) {
        assert o instanceof HostConnections : "Attachment should be a HostConnections";

        //System.out.println("============ TargetHandler :  connected ===============");

        HostConnections pool = (HostConnections) o;
        conn.getContext().setAttribute(PassThroughConstants.CONNECTION_POOL, pool);
        HttpRoute route = pool.getRoute();
          
        // create the connection information and set it to request ready
        org.siddhiesb.transport.passthru.TargetContext.create(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY, targetConfiguration);

        // notify the pool about the new connection
        targetConfiguration.getConnections().addConnection(conn);

        // notify about the new connection
        deliveryAgent.connected(pool.getRoute());
        
        HttpContext context = conn.getContext();
        context.setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME, System.currentTimeMillis());

        if (route.isTunnelled()) {
            // Requires a proxy tunnel
            ProxyTunnelHandler tunnelHandler = new ProxyTunnelHandler(route, connFactory);
            context.setAttribute(PassThroughConstants.TUNNEL_HANDLER, tunnelHandler);
        }
    }

    public void requestReady(NHttpClientConnection conn) {
        //System.out.println("============ TargetHandler :  requestReady ===============");

        HttpContext context = conn.getContext();
        org.siddhiesb.transport.passthru.ProtocolState connState = null;
        try {
            
            connState = org.siddhiesb.transport.passthru.TargetContext.getState(conn);

            if (connState == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE || connState == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY) {
                return;
            }

            if (connState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY) {
                handleInvalidState(conn, "Request not started");
                return;
            }

            ProxyTunnelHandler tunnelHandler = (ProxyTunnelHandler) context.getAttribute(PassThroughConstants.TUNNEL_HANDLER);
            if (tunnelHandler != null && !tunnelHandler.isCompleted()) {
                if (!tunnelHandler.isRequested()) {
                    HttpRequest request = tunnelHandler.generateRequest(context);
                    if (targetConfiguration.getProxyAuthenticator() != null) {
                        targetConfiguration.getProxyAuthenticator().authenticatePreemptively(request, context);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(conn + ": Sending CONNECT request to " + tunnelHandler.getProxy());
                    }
                    conn.submitRequest(request);
                    tunnelHandler.setRequested();
                }
                return;
            }
            
            TargetRequest request = org.siddhiesb.transport.passthru.TargetContext.getRequest(conn);
            if (request != null) {
                request.start(conn);
            }
            context.setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME, System.currentTimeMillis());
        } catch (IOException e) {
            logIOException(conn, e);
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);

        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);
        }
    }

    public void outputReady(NHttpClientConnection conn, ContentEncoder encoder) {
        //System.out.println("============ TargetHandler :  outputReady ===============");

        org.siddhiesb.transport.passthru.ProtocolState connState = null;
        try {
            connState = org.siddhiesb.transport.passthru.TargetContext.getState(conn);
            if (connState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD &&
                    connState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing message body");
                return;
            }

            TargetRequest request = org.siddhiesb.transport.passthru.TargetContext.getRequest(conn);
            if (request.hasEntityBody()) {
                int bytesWritten = request.write(conn, encoder);
            }
        } catch (IOException ex) {
            logIOException(conn, ex);
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSING);
            targetConfiguration.getConnections().shutdownConnection(conn);

            informWriterError(conn);

        } catch (Exception e) {
            log.error("Error occurred while writing data to the target", e);
            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);

            informWriterError(conn);
        }
    }

    public void responseReceived(NHttpClientConnection conn) {
        //System.out.println("============ TargetHandler :  responseReceived ===============");

        HttpContext context = conn.getContext();
        HttpResponse response = conn.getHttpResponse();
        org.siddhiesb.transport.passthru.ProtocolState connState;
        try {
            String method = null;
            ProxyTunnelHandler tunnelHandler = (ProxyTunnelHandler) context.getAttribute(PassThroughConstants.TUNNEL_HANDLER);
            if (tunnelHandler != null && !tunnelHandler.isCompleted()) {
                method = "CONNECT";
                context.removeAttribute(PassThroughConstants.TUNNEL_HANDLER);
                tunnelHandler.handleResponse(response, conn);
                if (tunnelHandler.isSuccessful()) {
                    log.debug(conn + ": Tunnel established");
                    conn.resetInput();
                    conn.requestOutput();
                    return;
                } else {
                    org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE);
                }
            }
            
        	context.setAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME, System.currentTimeMillis());
            connState = org.siddhiesb.transport.passthru.TargetContext.getState(conn);
            if (connState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Receiving response");
                return;
            }

            TargetRequest targetRequest = org.siddhiesb.transport.passthru.TargetContext.getRequest(conn);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < HttpStatus.SC_OK) {
                if (log.isDebugEnabled()) {
                    log.debug(conn + ": Received a 100 Continue response");
                }
                // Ignore 1xx response
                return;
            }

            if (targetRequest != null) {
                method = targetRequest.getMethod();
            }
            if (method == null) {
                method = "POST";
            }
            boolean canResponseHaveBody =
                    isResponseHaveBodyExpected(method, response);
            TargetResponse targetResponse = new TargetResponse(
                    targetConfiguration, response, conn, canResponseHaveBody);
            org.siddhiesb.transport.passthru.TargetContext.setResponse(conn, targetResponse);
            targetResponse.start(conn);

            CommonContext requestContext = org.siddhiesb.transport.passthru.TargetContext.get(conn).getCommonContext();

            targetConfiguration.getWorkerPool().execute(
                    new ClientWorker(requestContext, targetResponse, mediationEngine));

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);

            informReaderError(conn);

            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);
        }
    }


    public void inputReady(NHttpClientConnection conn, ContentDecoder decoder) {
        //System.out.println("============ TargetHandler :  inputReady ===============");

        org.siddhiesb.transport.passthru.ProtocolState connState;
        try {
            connState = org.siddhiesb.transport.passthru.TargetContext.getState(conn);
            if (connState.compareTo(org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD) < 0) {
                return;
            }
            if (connState != org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD &&
                    connState != org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY) {
                handleInvalidState(conn, "Response received");
                return;
            }

            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY);

            TargetResponse response = org.siddhiesb.transport.passthru.TargetContext.getResponse(conn);

			if (response != null) {
				int responseRead = response.read(conn, decoder);
			}
        } catch (IOException e) {
            logIOException(conn, e);

            informReaderError(conn);

            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);

            informReaderError(conn);

            org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn);
        }
    }

    public void closed(NHttpClientConnection conn) {
        //System.out.println("============ TargetHandler :  closed ===============");

        org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.TargetContext.getState(conn);
        
        boolean sendFault = false;

        if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY || state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": Keep-Alive Connection closed");
            }
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD || state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY) {
            informWriterError(conn);
            log.warn("Connection closed by target host while sending the request");
            sendFault = true;
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD || state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY) {
            informReaderError(conn);
            log.warn("Connection closed by target host while receiving the response");
            sendFault = false;
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
            informWriterError(conn);
            log.warn("Connection closed by target host before receiving the request");
            sendFault = true;
        }

        if (sendFault) {
        }

        org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
        targetConfiguration.getConnections().shutdownConnection(conn);
    }

    private void logIOException(NHttpClientConnection conn, IOException e) {
        String message = getErrorMessage("I/O error : " + e.getMessage(), conn);

        if (e instanceof ConnectionClosedException || (e.getMessage() != null &&
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed"))) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": I/O error (Probably the keep-alive connection " +
                        "was closed):" + e.getMessage());
            }
        } else if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("broken") != -1) {
                log.warn("I/O error (Probably the connection " +
                        "was closed by the remote party):" + e.getMessage());
            } else {
                log.error("I/O error: " + e.getMessage(), e);
            }
        } else {
            log.error(message, e);
        }
    }

    public void timeout(NHttpClientConnection conn) {
        //System.out.println("============ TargetHandler :  timeout ===============");

        org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.TargetContext.getState(conn);

        String message = getErrorMessage("Connection timeout", conn);
        if (log.isDebugEnabled()) {
            log.debug(conn + ": " + message);
        }

        if (state != null &&
                (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY || state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_DONE)) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": " + getErrorMessage("Keep-alive connection timed out", conn));
            }
        } else if (state != null ) {
            if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY) {
                informWriterError(conn);
            }

            if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY || state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD) {
                informReaderError(conn);
            }

            if (state.compareTo(org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) <= 0) {
            }
        }

        org.siddhiesb.transport.passthru.TargetContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
        targetConfiguration.getConnections().shutdownConnection(conn);
    }

    private boolean isResponseHaveBodyExpected(
            final String method, final HttpResponse response) {

        if ("HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        int status = response.getStatusLine().getStatusCode();
        return status >= HttpStatus.SC_OK
            && status != HttpStatus.SC_NO_CONTENT
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT;
    }

    /**
     * Include remote host and port information to an error message
     *
     * @param message the initial message
     * @param conn    the connection encountering the error
     * @return the updated error message
     */
    private String getErrorMessage(String message, NHttpClientConnection conn) {
        if (conn != null && conn instanceof DefaultNHttpClientConnection) {
            DefaultNHttpClientConnection c = ((DefaultNHttpClientConnection) conn);

            if (c.getRemoteAddress() != null) {
                return message + " For : " + c.getRemoteAddress().getHostAddress() + ":" +
                        c.getRemotePort();
            }
        }
        return message;
    }

    private void handleInvalidState(NHttpClientConnection conn, String action) {
        org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.TargetContext.getState(conn);

        if (log.isWarnEnabled()) {
            log.warn(conn + ": " + action + " while the handler is in an inconsistent state " +
                org.siddhiesb.transport.passthru.TargetContext.getState(conn));
        }
    }

    private void informReaderError(NHttpClientConnection conn) {
        org.siddhiesb.transport.passthru.Pipe reader = org.siddhiesb.transport.passthru.TargetContext.get(conn).getReader();

        if (reader != null) {
            reader.producerError();
        }
    }

    private void informWriterError(NHttpClientConnection conn) {
        Pipe writer = org.siddhiesb.transport.passthru.TargetContext.get(conn).getWriter();

        if (writer != null) {
            writer.consumerError();
        }
    }

    public void endOfInput(NHttpClientConnection conn) throws IOException {
        conn.close();
    }
    
    public void exception(NHttpClientConnection conn, Exception ex) {
        ProtocolState state = TargetContext.getState(conn);
        targetConfiguration.getConnections().shutdownConnection(conn);
    }

}
