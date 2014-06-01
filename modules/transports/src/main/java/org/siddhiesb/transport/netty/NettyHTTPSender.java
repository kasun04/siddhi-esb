package org.siddhiesb.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.TransportSenderAPI;

/**
 * Created by kasun on 4/23/14.
 */
public class NettyHTTPSender implements TransportSenderAPI {

    private volatile Channel outboundChannel;


    public void init(MediationEngineAPI mediationEngine) {

    }

    public void invoke(CommonContext commonContext) {
        String remoteHost = "localhost";
        int remotePort = 9000;

        ChannelHandlerContext ctx = (ChannelHandlerContext) commonContext.getProperty("NETTY_CTX");

        final Channel inboundChannel = ctx.channel();

        // Start the connection attempt.
        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ResponseChannel(inboundChannel))
                .option(ChannelOption.AUTO_READ, false);
        ChannelFuture f = b.connect(remoteHost, remotePort);
        outboundChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });


    }

    public void destroy() {

    }
}
