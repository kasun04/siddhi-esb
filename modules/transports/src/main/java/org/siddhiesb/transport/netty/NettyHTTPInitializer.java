package org.siddhiesb.transport.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.siddhiesb.common.api.MediationEngineAPI;

public class NettyHTTPInitializer extends ChannelInitializer<SocketChannel> {

    MediationEngineAPI mediationEngineAPI;

    public NettyHTTPInitializer(MediationEngineAPI mediationEngineAPI) {
        this.mediationEngineAPI = mediationEngineAPI;
    }

    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(new RequestChannel(mediationEngineAPI));
    }

}
