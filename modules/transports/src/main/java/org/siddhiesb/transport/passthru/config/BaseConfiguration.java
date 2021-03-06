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

package org.siddhiesb.transport.passthru.config;

/*import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;*/

import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.siddhiesb.transport.passthru.config.PassThroughConfiguration;
import org.siddhiesb.transport.passthru.util.BufferFactory;

import org.siddhiesb.transport.passthru.workerpool.WorkerPoolFactory;
import org.siddhiesb.transport.passthru.workerpool.WorkerPool;

/**
 * This class has common configurations for both sender and receiver.
 */
public abstract class BaseConfiguration {

    /** The thread pool for executing the messages passing through */
    private WorkerPool workerPool = null;

    /** Default http parameters */
    protected HttpParams httpParams = null;

    protected IOReactorConfig ioReactorConfig = null;

    protected BufferFactory bufferFactory = null;

    private int iOBufferSize;

    protected PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    public BaseConfiguration(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public void build() {
        iOBufferSize = conf.getIOBufferSize();
        if (workerPool == null) {
            workerPool = WorkerPoolFactory.getWorkerPool(conf.getWorkerPoolCoreSize(),
                    conf.getWorkerPoolMaxSize(),
                    conf.getWorkerThreadKeepaliveSec(),
                    conf.getWorkerPoolQueueLen(),
                    "gr_name",
                    "id");
        }

        httpParams = buildHttpParams();
        ioReactorConfig = buildIOReactorConfig();

        bufferFactory = new BufferFactory(iOBufferSize, new HeapByteBufferAllocator(), 512);
    }

    public int getIOBufferSize() {
        return iOBufferSize;
    }

    public WorkerPool getWorkerPool() {
        return workerPool;
    }

    protected HttpParams buildHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.
                setIntParameter(HttpConnectionParams.SO_TIMEOUT,
                        conf.getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000)).
                setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT,
                        conf.getIntProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 0)).
                setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                        conf.getIntProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)).
                setParameter(HttpProtocolParams.ORIGIN_SERVER,
                        conf.getStringProperty(HttpProtocolParams.ORIGIN_SERVER,  "WSO2-PassThrough-HTTP")).
                setParameter(HttpProtocolParams.USER_AGENT,
                        conf.getStringProperty(HttpProtocolParams.USER_AGENT, "Synapse-PT-HttpComponents-NIO"));

        return params;
    }

    protected IOReactorConfig buildIOReactorConfig() {
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(conf.getIOThreadsPerReactor());
        config.setSoTimeout(conf.getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000));
        config.setConnectTimeout(conf.getIntProperty(HttpConnectionParams.CONNECTION_TIMEOUT, 0));
        config.setTcpNoDelay(conf.getBooleanProperty(HttpConnectionParams.TCP_NODELAY, true));
        config.setSoLinger(conf.getIntProperty(HttpConnectionParams.SO_LINGER, -1));
        config.setSoReuseAddress(conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR, false));
        config.setInterestOpQueued(conf.getBooleanProperty("http.nio.interest-ops-queueing", false));
        config.setSelectInterval(conf.getIntProperty("http.nio.select-interval", 1000));
        return config;
    }
    
    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

}
