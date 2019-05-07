package netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import netty.hanlder.KoalasDecoder;
import netty.hanlder.KoalasEncoder;
import netty.hanlder.KoalasHandler;
import org.apache.thrift.TProcessor;
import server.config.AbstractKoalsServerPublisher;

import java.util.concurrent.ExecutorService;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private ExecutorService executorService;

    private  AbstractKoalsServerPublisher serverPublisher;

    public NettyServerInitiator(AbstractKoalsServerPublisher serverPublisher,ExecutorService executorService){
        this.serverPublisher = serverPublisher;
        this.executorService = executorService;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline ().addLast ( "decoder",new KoalasDecoder () );
        ch.pipeline ().addLast ( "encoder",new KoalasEncoder ());
        ch.pipeline ().addLast ( "handler",new KoalasHandler (serverPublisher,executorService) );
    }

}
