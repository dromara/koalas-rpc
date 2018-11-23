package netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import netty.hanlder.KoalasDecoder;
import netty.hanlder.KoalasEncoder;
import netty.hanlder.KoalasHandler;
import org.apache.thrift.TProcessor;

import java.util.concurrent.ExecutorService;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private TProcessor tProcessor;

    private ExecutorService executorService;

    private String privateKey;
    private String publicKey;

    public NettyServerInitiator(TProcessor tProcessor, ExecutorService executorService, String privateKey, String publicKey) {
        this.tProcessor = tProcessor;
        this.executorService = executorService;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline ().addLast ( "decoder",new KoalasDecoder () );
        ch.pipeline ().addLast ( "encoder",new KoalasEncoder ());
        ch.pipeline ().addLast ( "handler",new KoalasHandler (tProcessor,executorService,privateKey,publicKey) );
    }

}
