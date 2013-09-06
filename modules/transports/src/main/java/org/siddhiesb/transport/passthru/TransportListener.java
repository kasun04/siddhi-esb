package org.siddhiesb.transport.passthru;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.TransportListenerAPI;
import org.siddhiesb.transport.http.conn.Scheme;
import org.siddhiesb.transport.http.conn.ServerConnFactory;
import org.siddhiesb.transport.passthru.config.SourceConfiguration;
import org.siddhiesb.transport.passthru.workerpool.NativeThreadFactory;
import org.siddhiesb.transport.passthru.workerpool.WorkerPool;
import org.siddhiesb.transport.passthru.workerpool.WorkerPoolFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TransportListener implements TransportListenerAPI {

    protected Log log;

    /** The reactor being used */
    private DefaultListeningIOReactor ioReactor;
    /** The I/O dispatch */
    private org.siddhiesb.transport.passthru.ServerIODispatch ioEventDispatch;
    /** The protocol handler */
    private org.siddhiesb.transport.passthru.SourceHandler handler;
    /** The connection factory */
    private ServerConnFactory serverConnFactory;
    /** The protocol scheme of the listener*/
    private Scheme scheme;
    /** The configuration of the listener */
    private SourceConfiguration sourceConfiguration = null;

    /** The custom URI map for the services if there are any */
    private Map<String, String> serviceNameToEPRMap = new HashMap<String, String>();
    /** The service name map for the custom URI if there are any */
    private Map<String, String> eprToServiceNameMap = new HashMap<String, String>();
    /** the axis observer that gets notified of service life cycle events*/

    private volatile int state;

    /** Active Connection Monitor Scheduler **/
    private final ScheduledExecutorService activeConnectionMonitorScheduler = Executors.newSingleThreadScheduledExecutor();

    /** Delay for ActiveConnectionMonitor **/
    public static final long ACTIVE_CONNECTION_MONITOR_DELAY = 1000;

    public static final int WORKER_COUNT = 2;


    /*Modified Class variables*/
    private WorkerPool workerPool;


    public TransportListener() {
        log = LogFactory.getLog(this.getClass().getName());
    }

    public void init(MediationEngineAPI mediationEngineAPI) {

        workerPool = WorkerPoolFactory.getWorkerPool(400, 500, 60, -1, "trp-listener", "trp-listener");

        /*Create Source Config*/
        sourceConfiguration = new SourceConfiguration(null, workerPool);
        sourceConfiguration.build();

        /*Create HTTP Params*/

        HttpParams httpParams = new SyncBasicHttpParams();
        httpParams
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1")
                .setParameter(CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1");

        /*Server connection factory*/
        serverConnFactory = new ServerConnFactory(httpParams);



        // Create source handler
        handler = new org.siddhiesb.transport.passthru.SourceHandler(sourceConfiguration, mediationEngineAPI);
        ioEventDispatch = new ServerIODispatch(handler, serverConnFactory);

    }

    public void start() {
        try {
            ioReactor = new DefaultListeningIOReactor(sourceConfiguration.getIOReactorConfig(),
                    new NativeThreadFactory(new ThreadGroup("trp-listener" + " thread group"), "trp-listener"));

            /*IOReactor Exception Handling*/
            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {

                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: " + "Trp-Listener" +
                            " ListeningIOReactor encountered a checked exception : " +
                            ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: " + "Trp-Listener" +
                            " ListeningIOReactor encountered a runtime exception : "
                            + runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOReactorException e) {
            e.printStackTrace();
        }

        /*Start IOReactor on a separate thread*/

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (Exception e) {
                    log.fatal("Exception encountered in the " + "Trp-Listener" + " Listener. " +
                            "No more connections will be accepted by this transport", e);
                }
                log.info("Trp-Listener" + " Listener shutdown.");
            }
        }, "PassThrough" + "Trp-Listener" + "Listener");
        t.start();

        /*Start Listening for connections*/
        int port = 7070;
        ioReactor.listen(new InetSocketAddress(port));

    }


    public void stop() {

    }







}
