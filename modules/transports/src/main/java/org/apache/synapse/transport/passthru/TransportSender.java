package org.apache.synapse.transport.passthru;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.synapse.common.api.MediationEngineAPI;
import org.apache.synapse.common.api.PassThruContext;
import org.apache.synapse.common.api.TransportSenderAPI;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.http.conn.ProxyConfig;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.util.SourceResponseFactory;
import org.apache.synapse.transport.passthru.workerpool.NativeThreadFactory;
import org.apache.synapse.transport.passthru.workerpool.WorkerPool;
import org.apache.synapse.transport.passthru.workerpool.WorkerPoolFactory;

import java.io.IOException;

public class TransportSender implements TransportSenderAPI {
    protected Log log;

    /** IOReactor used to create connections and manage them */
    private DefaultConnectingIOReactor connectingIOReactor;
    /** Protocol handler */
    private TargetHandler handler;
    /** I/O dispatcher */
    private IOEventDispatch ioEventDispatch;
    /** The connection factory */
    private ClientConnFactory clientConnFactory;

    /** Delivery agent used for delivering the messages to the servers */
    private DeliveryAgent deliveryAgent;

    /** The protocol scheme of the sender */
    private Scheme scheme;
    /** The configuration of the sender */
    private TargetConfiguration targetConfiguration;


    public TransportSender() {
        log = LogFactory.getLog(this.getClass().getName());
    }

    public void init(MediationEngineAPI mediationEngine) {
        WorkerPool workerPool = WorkerPoolFactory.getWorkerPool(20, 40, 10000, 1000, "trp-sender", "trp-sender");

        /*Creating Target Config with null Authenticator*/
        targetConfiguration = new TargetConfiguration(workerPool, null);
        targetConfiguration.build();


        /*HTTP Params for outbound connections*/
        HttpParams httpParams = new SyncBasicHttpParams();
        httpParams
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1")
                .setParameter(CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1");
        /*Creating clientCon factory*/
        clientConnFactory = new ClientConnFactory(httpParams);


        try {
            /*Starting the connecting IOReactor*/
            connectingIOReactor = new DefaultConnectingIOReactor(
                    targetConfiguration.getIOReactorConfig(),
                    new NativeThreadFactory(new ThreadGroup("ptt-sender" + " Thread Group"), ""));
            connectingIOReactor.setExceptionHandler(new IOReactorExceptionHandler() {

                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: " + "ptt-sender" +
                            " ConnectingIOReactor encountered a checked exception : " +
                            ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: " + "ptt-sender" +
                            " ConnectingIOReactor encountered a runtime exception : "
                            + runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });

        } catch (IOReactorException e) {
            e.printStackTrace();
        }


        ConnectCallback connectCallback = new ConnectCallback();
        TargetConnections targetConnections = new TargetConnections(connectingIOReactor, targetConfiguration, connectCallback);
        targetConfiguration.setConnections(targetConnections);
        // create the delivery agent to hand over messages

        deliveryAgent = new DeliveryAgent(targetConfiguration, targetConnections,null);
        connectCallback.setDeliveryAgent(deliveryAgent);

        handler = new TargetHandler(deliveryAgent, clientConnFactory, targetConfiguration, mediationEngine);
        ioEventDispatch = new ClientIODispatch(handler, clientConnFactory);

        // start the sender in a separate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    connectingIOReactor.execute(ioEventDispatch);
                } catch (Exception ex) {
                    log.fatal("Exception encountered in the " + "ptt-sender" + " Sender. " +
                            "No more connections will be initiated by this transport", ex);
                }
                log.info("ptt-sender" + " Sender shutdown");
            }
        }, "PassThrough" + "ptt-sender" + "Sender");
        t.start();
    }


    public void invoke(PassThruContext passThruContext) {

        if ("TRUE".equals((String) passThruContext.getProperty("RESPONSE"))) {
            /*response path*/
            submitResponse(passThruContext);
        } else {
            String endpointAddress = "http://localhost:9000/services/SimpleStockQuoteService";
            passThruContext.setProperty("To", endpointAddress);
            /*ToDo: Check to PT Pipe*/
            deliveryAgent.submit(passThruContext);

            /*submit request ? */
        }

    }

    public void submitResponse(PassThruContext passThruContext) {
        NHttpServerConnection conn = (NHttpServerConnection) passThruContext.getProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
        SourceConfiguration sourceConfiguration = (SourceConfiguration) passThruContext.getProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION);

        SourceRequest sourceRequest = SourceContext.getRequest(conn);
        SourceResponse sourceResponse = SourceResponseFactory.create(passThruContext,
                sourceRequest, sourceConfiguration);
        SourceContext.setResponse(conn, sourceResponse);
        Pipe pipe = (Pipe) passThruContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);

        pipe.attachConsumer(conn);
        sourceResponse.connect(pipe);

        conn.requestOutput();
    }


}
