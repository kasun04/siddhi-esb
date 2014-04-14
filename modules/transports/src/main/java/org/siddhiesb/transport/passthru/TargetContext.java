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

import org.apache.http.nio.NHttpConnection;
import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.transport.passthru.config.TargetConfiguration;
import java.nio.ByteBuffer;

/**
 * When a connection is created, an object of this class is stored in the Connection Context.
 * It is used as a holder for information required during the life-cycle of this connection.
 */
public class TargetContext {
    private TargetConfiguration targetConfiguration = null;

    public static final String CONNECTION_INFORMATION = "CONNECTION_INFORMATION";
    /** The request for this connection */
    private TargetRequest request;
    /** The response for this connection */
    private TargetResponse response;
    /** State of the connection */
    private org.siddhiesb.transport.passthru.ProtocolState state;

    /** The current reader */
    private org.siddhiesb.transport.passthru.Pipe reader;
    /** The current writer */
    private org.siddhiesb.transport.passthru.Pipe writer;

    /*Request PTContext - Used for co-relation*/
    private CommonContext commonContext;

    public TargetContext(TargetConfiguration targetConfiguration) {
        this.targetConfiguration = targetConfiguration;
    }

    public org.siddhiesb.transport.passthru.ProtocolState getState() {
        return state;
    }

    public void setState(org.siddhiesb.transport.passthru.ProtocolState state) {
        this.state = state;
    }

    public TargetRequest getRequest() {
        return request;
    }

    public void setRequest(TargetRequest request) {
        this.request = request;
    }

    public TargetResponse getResponse() {
        return response;
    }

    public void setResponse(TargetResponse response) {
        this.response = response;
    }

    public org.siddhiesb.transport.passthru.Pipe getReader() {
        return reader;
    }

    public org.siddhiesb.transport.passthru.Pipe getWriter() {
        return writer;
    }

    public void setReader(org.siddhiesb.transport.passthru.Pipe reader) {
        this.reader = reader;
    }

    public void setWriter(org.siddhiesb.transport.passthru.Pipe writer) {
        this.writer = writer;
    }

    public void reset() {
        request = null;
        response = null;
        state = org.siddhiesb.transport.passthru.ProtocolState.REQUEST_READY;

        if (writer != null) {
            ByteBuffer buffer = writer.getBuffer();
            buffer.clear();
            targetConfiguration.getBufferFactory().release(buffer);
        }

        reader = null;
        writer = null;       
    }

    public static void create(NHttpConnection conn, org.siddhiesb.transport.passthru.ProtocolState state,
                              TargetConfiguration configuration) {
        TargetContext info = new TargetContext(configuration);

        conn.getContext().setAttribute(CONNECTION_INFORMATION, info);

        info.setState(state);
    }

    public static void updateState(NHttpConnection conn, org.siddhiesb.transport.passthru.ProtocolState state) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        if (info != null) {
            info.setState(state);
        }  else {
            throw new IllegalStateException("Connection information should be present");
        }
    }

    public static boolean assertState(NHttpConnection conn, org.siddhiesb.transport.passthru.ProtocolState state) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        return info != null && info.getState() == state;

    }

    public static ProtocolState getState(NHttpConnection conn) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        return info != null ? info.getState() : null;
    }

    public static void setRequest(NHttpConnection conn, TargetRequest request) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        if (info != null) {
            info.setRequest(request);
        } else {
            throw new IllegalStateException("Connection information should be present");
        }
    }

    public static void setResponse(NHttpConnection conn, TargetResponse response) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        if (info != null) {
            info.setResponse(response);
        } else {
            throw new IllegalStateException("Connection information should be present");
        }
    }

    public static TargetRequest getRequest(NHttpConnection conn) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        return info != null ? info.getRequest() : null;
    }

    public static TargetResponse getResponse(NHttpConnection conn) {
        TargetContext info = (TargetContext)
                conn.getContext().getAttribute(CONNECTION_INFORMATION);

        return info != null ? info.getResponse() : null;
    }

    public static TargetContext get(NHttpConnection conn) {
        return (TargetContext) conn.getContext().getAttribute(CONNECTION_INFORMATION);
    }

    public CommonContext getCommonContext() {
        return commonContext;
    }

    public void setCommonContext(CommonContext commonContext) {
        this.commonContext = commonContext;
    }
}
