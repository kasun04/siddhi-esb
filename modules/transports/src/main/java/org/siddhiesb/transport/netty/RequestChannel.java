package org.siddhiesb.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.siddhiesb.common.api.CommonContext;
import org.siddhiesb.common.api.DefaultCommonContext;
import org.siddhiesb.common.api.MediationEngineAPI;


public class RequestChannel extends ChannelHandlerAdapter {
    private MediationEngineAPI mediationEngine;

    private volatile Channel outboundChannel;
    NettyHTTPSender nettyHTTPSender;

    public RequestChannel(MediationEngineAPI mediationEngine) {
        this.mediationEngine = mediationEngine;

        nettyHTTPSender = new NettyHTTPSender();
        this.mediationEngine.init(nettyHTTPSender);

    }


    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        /*Begin My simple mediation engine */
        String remoteHost = "localhost";
        int remotePort = 9000;

        CommonContext commonContext = new DefaultCommonContext();
        commonContext.setProperty(NettyHTTPTransportConstants.NETTY_CTX, ctx);

        mediationEngine.process(commonContext);
        /*End My simple mediation engine */

        /*final Channel inboundChannel = ctx.channel();

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
        });*/

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }

    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (outboundChannel.isActive()) {
            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // was able to flush out data, start to read the next chunk
                        ctx.channel().read();
                    } else {
                        future.channel().close();
                    }
                }
            });
        }

    }





    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }


    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
