package org.siddhiesb.transport.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;


public class ResponseChannel extends ChannelHandlerAdapter {

    private final Channel inboundChannel;

    public ResponseChannel(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }


    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
        ctx.write(Unpooled.EMPTY_BUFFER);
    }

    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        RequestChannel.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        RequestChannel.closeOnFlush(ctx.channel());
    }


}
