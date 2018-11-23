package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import netty.initializer.NettyServerInitiator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import register.ZookeeperServer;
import server.IkoalasServer;
import server.KoalasDefaultThreadFactory;
import server.config.AbstractKoalsServerPublisher;
import server.config.ZookServerConfig;
import utils.KoalasThreadedSelectorWorkerExcutorUtil;

import java.util.concurrent.ExecutorService;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class NettyServer implements IkoalasServer {
    private final static Logger logger = LoggerFactory.getLogger ( NettyServer.class );

    private AbstractKoalsServerPublisher serverPublisher;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ExecutorService executorService;
    private ZookeeperServer zookeeperServer;

    public NettyServer(AbstractKoalsServerPublisher serverPublisher) {
        this.serverPublisher = serverPublisher;
    }

    @Override
    public void run() {
        try {
            if (Epoll.isAvailable ()) {
                bossGroup = new EpollEventLoopGroup (serverPublisher.bossThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS:serverPublisher.bossThreadCount);
                workerGroup = new EpollEventLoopGroup ( serverPublisher.workThreadCount==0? AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS*2:serverPublisher.workThreadCount);
            } else {
                bossGroup = new NioEventLoopGroup (serverPublisher.bossThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS:serverPublisher.bossThreadCount);
                workerGroup = new NioEventLoopGroup ( serverPublisher.workThreadCount==0? AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS*2:serverPublisher.workThreadCount );
            }
            executorService = KoalasThreadedSelectorWorkerExcutorUtil.getWorkerExecutorWithQueue (serverPublisher.koalasThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_KOALAS_THREADS:serverPublisher.koalasThreadCount,serverPublisher.koalasThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_KOALAS_THREADS:serverPublisher.koalasThreadCount,serverPublisher.workQueue,new KoalasDefaultThreadFactory (serverPublisher.serviceInterface.getName ()));

            ServerBootstrap b = new ServerBootstrap ();
            b.group ( bossGroup, workerGroup ).channel ( workerGroup instanceof EpollEventLoopGroup ? EpollServerSocketChannel.class : NioServerSocketChannel.class )
                    .handler ( new LoggingHandler ( LogLevel.INFO ) )
                    .childHandler ( new NettyServerInitiator (serverPublisher.getTProcessor (),executorService,serverPublisher.getPrivateKey (),serverPublisher.getPublicKey ()))
                    .option ( ChannelOption.SO_BACKLOG, 1024 )
                    .option ( ChannelOption.SO_REUSEADDR, true )
                    .option ( ChannelOption.SO_KEEPALIVE, true );
            Channel ch = b.bind ( serverPublisher.port ).sync ().channel ();
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run(){
                    logger.info ( "Shutdown by Runtime" );
                    if(zookeeperServer != null){
                        zookeeperServer.destroy ();
                    }
                    logger.info ( "wait for service over 3000ms" );
                    try {
                        Thread.sleep ( 3000 );
                    } catch (Exception e) {
                    }
                    if(executorService!=null){
                        executorService.shutdown ();
                    }
                    if(bossGroup != null) bossGroup.shutdownGracefully ();
                    if(workerGroup != null) workerGroup.shutdownGracefully ();
                }
            });

            if(StringUtils.isNotEmpty ( serverPublisher.zkpath )){
                ZookServerConfig zookServerConfig = new ZookServerConfig ( serverPublisher.zkpath,serverPublisher.serviceInterface.getName (),serverPublisher.env,serverPublisher.port,serverPublisher.weight,"netty" );
                zookeeperServer = new ZookeeperServer ( zookServerConfig );
                zookeeperServer.init ();
            }
        } catch ( Exception e){
            logger.error ( "NettyServer start faid !",e );
            if(bossGroup != null) bossGroup.shutdownGracefully ();
            if(workerGroup != null) workerGroup.shutdownGracefully ();
        }

        logger.info("netty server init success server={}",serverPublisher);

    }

    @Override
    public void stop() {
        logger.info ( "Shutdown by stop" );
        if(zookeeperServer != null){
            zookeeperServer.destroy ();
        }
        logger.info ( "wait for service over 3000ms" );
        try {
            Thread.sleep ( 3000 );
        } catch (Exception e) {
        }
        if(executorService!=null){
            executorService.shutdown ();
        }
        if(bossGroup != null) bossGroup.shutdownGracefully ();
        if(workerGroup != null) workerGroup.shutdownGracefully ();

        logger.info("netty server stop success server={}",serverPublisher);
    }
}
