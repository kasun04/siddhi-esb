/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.siddhiesb.transport.http.conn;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpMessageWriter;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.siddhiesb.transport.http.access.AccessHandler;
import org.siddhiesb.transport.http.conn.*;
import org.siddhiesb.transport.http.conn.LoggingIOSession;
import org.siddhiesb.transport.http.conn.LoggingUtils;

public class LoggingNHttpServerConnection extends DefaultNHttpServerConnection
        implements org.siddhiesb.transport.http.conn.UpgradableNHttpConnection {

    private static final AtomicLong COUNT = new AtomicLong();

    private final Log log;
    private final Log iolog;
    private final Log headerlog;
    private final Log accesslog;
    private final Log wirelog;
    private final String id;

    private IOSession original;

    public LoggingNHttpServerConnection(
            final IOSession session,
            final HttpRequestFactory requestFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(session, requestFactory, allocator, params);
        this.log = LogFactory.getLog(getClass());
        this.iolog = LogFactory.getLog(session.getClass());
        this.headerlog = LogFactory.getLog(org.siddhiesb.transport.http.conn.LoggingUtils.HEADER_LOG_ID);
        this.wirelog = LogFactory.getLog(org.siddhiesb.transport.http.conn.LoggingUtils.WIRE_LOG_ID);
        this.accesslog = LogFactory.getLog(LoggingUtils.ACCESS_LOG_ID);
        this.id = "http-incoming-" + COUNT.incrementAndGet();
        this.original = session;
        if (this.iolog.isDebugEnabled() || this.wirelog.isDebugEnabled()) {
            super.bind(new org.siddhiesb.transport.http.conn.LoggingIOSession(session, this.id, this.iolog, this.wirelog));
        }
    }

    @Override
    public void close() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Close connection");
        }
        super.close();
    }

    @Override
    public void shutdown() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Shutdown connection");
        }
        super.shutdown();
    }

    @Override
    public void submitResponse(final HttpResponse response) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": "  + response.getStatusLine().toString());
        }
        super.submitResponse(response);
    }

    @Override
    public void consumeInput(final NHttpServerEventHandler handler) {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Consume input");
        }
        super.consumeInput(handler);
    }

    @Override
    public void produceOutput(final NHttpServerEventHandler handler) {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Produce output");
        }
        super.produceOutput(handler);
    }

    @Override
    protected NHttpMessageWriter<HttpResponse> createResponseWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        return new LoggingNHttpMessageWriter(
                super.createResponseWriter(buffer, params));
    }

    @Override
    protected NHttpMessageParser<HttpRequest> createRequestParser(
            final SessionInputBuffer buffer,
            final HttpRequestFactory requestFactory,
            final HttpParams params) {
        return new LoggingNHttpMessageParser(
                super.createRequestParser(buffer, requestFactory, params));
    }

    @Override
    public String toString() {
        return this.id;
    }

    public IOSession getIOSession() {
        return this.original;
    }

    @Override
    public void bind(final IOSession session) {
        this.original = session;
        if (this.iolog.isDebugEnabled() || this.wirelog.isDebugEnabled()) {
            super.bind(new org.siddhiesb.transport.http.conn.LoggingIOSession(session, this.id, this.iolog, this.wirelog));
        } else {
            super.bind(session);
        }
    }

    class LoggingNHttpMessageWriter implements NHttpMessageWriter<HttpResponse> {

        private final NHttpMessageWriter<HttpResponse> writer;

        public LoggingNHttpMessageWriter(final NHttpMessageWriter<HttpResponse> writer) {
            super();
            this.writer = writer;
        }

        public void reset() {
            this.writer.reset();
        }

        public void write(final HttpResponse message) throws IOException, HttpException {
            if (message != null && headerlog.isDebugEnabled()) {
                headerlog.debug(id + " << " + message.getStatusLine().toString());
                Header[] headers = message.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    headerlog.debug(id + " << " + headers[i].toString());
                }
            }

            if (message != null && accesslog.isInfoEnabled()) {
                AccessHandler.getAccess().addAccessToQueue(message);
            }

            this.writer.write(message);
        }

    }

    class LoggingNHttpMessageParser implements NHttpMessageParser<HttpRequest> {

        private final NHttpMessageParser<HttpRequest> parser;

        public LoggingNHttpMessageParser(final NHttpMessageParser<HttpRequest> parser) {
            super();
            this.parser = parser;
        }

        public void reset() {
            this.parser.reset();
        }

        public int fillBuffer(final ReadableByteChannel channel) throws IOException {
            return this.parser.fillBuffer(channel);
        }

        public HttpRequest parse() throws IOException, HttpException {
            HttpRequest message = this.parser.parse();
            if (message != null && headerlog.isDebugEnabled()) {
                headerlog.debug(id + " >> " + message.getRequestLine().toString());
                Header[] headers = message.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    headerlog.debug(id + " >> " + headers[i].toString());
                }
            }
            if (message != null && accesslog.isInfoEnabled()) {
                AccessHandler.getAccess().addAccessToQueue(message);
            }

            return message;
        }

    }

}