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

import java.io.IOException;
import java.io.OutputStream;

/*import org.apache.axis2.AxisFault;*/
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleOutputBuffer;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.siddhiesb.transport.passthru.*;
import org.siddhiesb.transport.passthru.Pipe;
import org.siddhiesb.transport.passthru.ProtocolState;
import org.siddhiesb.transport.passthru.SourceContext;
import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.transport.passthru.config.SourceConfiguration;

/**
 * This is the class where transport interacts with the client. This class
 * receives events for a particular connection. These events give information
 * about the message and its various states.
 */
public class SourceHandler implements NHttpServerEventHandler {
    private static Log log = LogFactory.getLog(SourceHandler.class);

    private final SourceConfiguration sourceConfiguration;

    private MediationEngineAPI mediationEngine;


    public SourceHandler(SourceConfiguration sourceConfiguration, MediationEngineAPI mediationEngineAPI) {
        this.sourceConfiguration = sourceConfiguration;
        mediationEngine = mediationEngineAPI;
    }

    public void connected(NHttpServerConnection conn) {
        System.out.println("============ Connected ===============");
        // we have to have these two operations in order
        sourceConfiguration.getSourceConnections().addConnection(conn);
        org.siddhiesb.transport.passthru.SourceContext.create(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY, sourceConfiguration);
    }

    public void requestReceived(NHttpServerConnection conn) {
        try {

            //System.out.println("============ SourceHandler : requestReceived ===============");

            HttpContext _context = conn.getContext();
        	_context.setAttribute(PassThroughConstants.REQ_ARRIVAL_TIME, System.currentTimeMillis());
        	 
            if (!org.siddhiesb.transport.passthru.SourceContext.assertState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY) && !org.siddhiesb.transport.passthru.SourceContext.assertState(conn, org.siddhiesb.transport.passthru.ProtocolState.WSDL_RESPONSE_DONE)) {
                handleInvalidState(conn, "Request received");
                return;
            }
            // we have received a message over this connection. So we must inform the pool
            sourceConfiguration.getSourceConnections().useConnection(conn);

            // at this point we have read the HTTP Headers
            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD);

            SourceRequest request = new SourceRequest(
                    sourceConfiguration, conn.getHttpRequest(), conn);

            org.siddhiesb.transport.passthru.SourceContext.setRequest(conn, request);

            request.start(conn);

            String method = request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase():"";
            OutputStream os = null;
            if ("GET".equals(method) || "HEAD".equals(method)) {
				HttpContext context = request.getConnection().getContext();
				ContentOutputBuffer outputBuffer = new SimpleOutputBuffer(8192,	new HeapByteBufferAllocator());
				context.setAttribute("synapse.response-source-buffer",outputBuffer);
				os = new ContentOutputStream(outputBuffer);
			} 

            sourceConfiguration.getWorkerPool().execute(
                    new org.siddhiesb.transport.passthru.ServerWorker(request, sourceConfiguration,os, mediationEngine));
        } catch (HttpException e) {
            log.error(e.getMessage(), e);

            informReaderError(conn);

            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        } catch (IOException e) {
            logIOException(conn, e);

            informReaderError(conn);

            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    public void inputReady(NHttpServerConnection conn,
                           ContentDecoder decoder) {
        //System.out.println("============ SourceHandler :  inputReady ===============");

        org.siddhiesb.transport.passthru.ProtocolState protocolState = org.siddhiesb.transport.passthru.SourceContext.getState(conn);

        if (protocolState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD
                && protocolState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY) {
            handleInvalidState(conn, "Request message body data received");
            return;
        }

        org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY);
        SourceRequest request = org.siddhiesb.transport.passthru.SourceContext.getRequest(conn);
        try {
            int readBytes = request.read(conn, decoder);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void responseReady(NHttpServerConnection conn) {
        try {
            org.siddhiesb.transport.passthru.ProtocolState protocolState = org.siddhiesb.transport.passthru.SourceContext.getState(conn);
            if (protocolState.compareTo(org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) < 0) {
                return;
            }

            if (protocolState.compareTo(org.siddhiesb.transport.passthru.ProtocolState.CLOSING) >= 0) {
                return;
            }

            if (protocolState != org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing a response");
                return;
            }

            // because the duplex nature of http core we can reach hear without a actual response
            SourceResponse response = org.siddhiesb.transport.passthru.SourceContext.getResponse(conn);
            if (response != null) {
                response.start(conn);
            }
        } catch (IOException e) {
            logIOException(conn, e);
            informWriterError(conn);
            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);

        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            informWriterError(conn);
            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    public void outputReady(NHttpServerConnection conn,
                            ContentEncoder encoder) {
        try {
            org.siddhiesb.transport.passthru.ProtocolState protocolState = org.siddhiesb.transport.passthru.SourceContext.getState(conn);
            
            //special case to handle WSDLs
            if(protocolState == org.siddhiesb.transport.passthru.ProtocolState.WSDL_RESPONSE_DONE){
            	// we need to shut down if the shutdown flag is set
            	 HttpContext context = conn.getContext();
            	 ContentOutputBuffer outBuf = (ContentOutputBuffer) context.getAttribute(
                         "synapse.response-source-buffer");
            	  int bytesWritten = outBuf.produceContent(encoder);
                  conn.requestInput();
                  if(outBuf instanceof SimpleOutputBuffer && !((SimpleOutputBuffer)outBuf).hasData()){
                	  sourceConfiguration.getSourceConnections().releaseConnection(conn);
                  }
                  
            	return;
            }

            if (protocolState != org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD
                    && protocolState != org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY) {
                log.warn("Illegal incoming connection state: "
                        + protocolState + " . Possibly two send backs " +
                        "are happening for the same request");

                handleInvalidState(conn, "Trying to write response body");
                return;
            }

            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY);

            SourceResponse response = org.siddhiesb.transport.passthru.SourceContext.getResponse(conn);

            int bytesSent = response.write(conn, encoder);
            
			if (encoder.isCompleted()) {
				HttpContext context = conn.getContext();
				if (context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME) != null &&
				    context.getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME) != null &&
				    context.getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME) != null) {
				}

				context.removeAttribute(PassThroughConstants.REQ_ARRIVAL_TIME);
				context.removeAttribute(PassThroughConstants.REQ_DEPARTURE_TIME);
				context.removeAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME);
			}
            
        } catch (IOException e) {
            logIOException(conn, e);

            informWriterError(conn);

            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        } 
    }

    private void logIOException(NHttpServerConnection conn, IOException e) {
        // this check feels like crazy! But weird things happened, when load testing.
        if (e == null) {
            return;
        }
        if (e instanceof ConnectionClosedException || (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed")))) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": I/O error (Probably the keepalive connection " +
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
            log.error("Unexpected I/O error: " + e.getClass().getName(), e);
        }
    }

    public void timeout(NHttpServerConnection conn) {
        org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.SourceContext.getState(conn);

        if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY || state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": Keep-Alive connection was time out: " + conn);
            }
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY ||
                state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD) {
            informReaderError(conn);
            log.warn("Connection time out while reading the request: " + conn);
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY ||
                state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
            log.warn("Connection time out while writing the response: " + conn);
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE){
            log.warn("Connection time out after request is read: " + conn);
        }

        org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    public void closed(NHttpServerConnection conn) {
        org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.SourceContext.getState(conn);

        if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY || state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": Keep-Alive connection was closed: " + conn);
            }
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY ||
                state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD) {
            informReaderError(conn);
            log.warn("Connection closed while reading the request: " + conn);
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY ||
                state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
            log.warn("Connection closed while writing the response: " + conn);
        } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
            log.warn("Connection closed by the client after request is read: " + conn);
        }

        org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    public void endOfInput(NHttpServerConnection conn) throws IOException {
        conn.close();
    }

    public void exception(NHttpServerConnection conn, Exception ex) {
        if (ex instanceof IOException) {
            logIOException(conn, (IOException) ex);

            org.siddhiesb.transport.passthru.ProtocolState state = org.siddhiesb.transport.passthru.SourceContext.getState(conn);
            if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_BODY ||
                    state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_HEAD) {
                informReaderError(conn);
            } else if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_BODY ||
                    state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_HEAD) {
                informWriterError(conn);
            } else if (state == org.siddhiesb.transport.passthru.ProtocolState.REQUEST_DONE) {
                informWriterError(conn);
            } else if (state == org.siddhiesb.transport.passthru.ProtocolState.RESPONSE_DONE) {
                informWriterError(conn);
            }
            
            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        } else if (ex instanceof HttpException) {
            try {
                if (conn.isResponseSubmitted()) {
                    sourceConfiguration.getSourceConnections().shutDownConnection(conn);
                    return;
                }
                HttpContext httpContext = conn.getContext();

                HttpResponse response = new BasicHttpResponse(
                        HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad request");
                response.setParams(
                        new DefaultedHttpParams(sourceConfiguration.getHttpParams(),
                                response.getParams()));
                response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

                // Pre-process HTTP request
                httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
                httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, null);
                httpContext.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

                sourceConfiguration.getHttpProcessor().process(response, httpContext);

                conn.submitResponse(response);            
                org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
                conn.close();
            } catch (Exception ex1) {
                log.error(ex.getMessage(), ex);
                org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
                sourceConfiguration.getSourceConnections().shutDownConnection(conn);
            }
        } else {
            log.error("Unexoected error: " + ex.getMessage(), ex);
            org.siddhiesb.transport.passthru.SourceContext.updateState(conn, org.siddhiesb.transport.passthru.ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    private void handleInvalidState(NHttpServerConnection conn, String action) {
        log.warn(action + " while the handler is in an inconsistent state " +
                org.siddhiesb.transport.passthru.SourceContext.getState(conn));
        org.siddhiesb.transport.passthru.SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn);
    }

    private void informReaderError(NHttpServerConnection conn) {
        org.siddhiesb.transport.passthru.Pipe reader = org.siddhiesb.transport.passthru.SourceContext.get(conn).getReader();

        if (reader != null) {
            reader.producerError();
        }
    }

    private void informWriterError(NHttpServerConnection conn) {
        Pipe writer = SourceContext.get(conn).getWriter();

        if (writer != null) {
            writer.consumerError();
        }
    }
    
    /**
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out. This method hides any exceptions and is targetted
     * for non critical (i.e. browser requests etc) requests, which are not core messages
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     */
    public void commitResponseHideExceptions(
            final NHttpServerConnection conn, final HttpResponse response) {
        try {
            conn.suspendInput();
            sourceConfiguration.getHttpProcessor().process(response, conn.getContext());
            conn.submitResponse(response);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error : " + e.getMessage(), e, conn);
        } catch (IOException e) {
            handleException("IO error submiting response : " + e.getMessage(), e, conn);
        }
    }
    
    
    private void handleException(String msg, Exception e, NHttpServerConnection conn) {
        log.error(msg, e);
        if (conn != null) {
            //shutdownConnection(conn);
        }
    }
    
    
    
}
