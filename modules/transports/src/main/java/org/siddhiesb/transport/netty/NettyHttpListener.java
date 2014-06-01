package org.siddhiesb.transport.netty;

import org.siddhiesb.common.api.MediationEngineAPI;
import org.siddhiesb.common.api.TransportListenerAPI;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyHttpListener implements TransportListenerAPI {

    MediationEngineAPI mediationEngine;
    private final int localPort;

    public NettyHttpListener() {
        localPort = 7070;
    }

    public void init(MediationEngineAPI mediationEngineAPI) {
        mediationEngine = mediationEngineAPI;
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyHTTPInitializer(mediationEngine))
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(localPort).sync().channel().closeFuture().sync();
        } catch (Exception e) {
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }



    }

    public void stop() {

    }

    public static void main(String[] args) {
        new NettyHttpListener().start();
    }
}
