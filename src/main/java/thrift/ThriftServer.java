package thrift;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import register.ZookeeperServer;
import server.IkoalasServer;
import server.KoalasDefaultThreadFactory;
import server.config.AbstractKoalsServerPublisher;
import server.config.ZookServerConfig;
import transport.TKoalasFramedTransport;
import utils.KoalasThreadedSelectorWorkerExcutorUtil;

import java.util.concurrent.ExecutorService;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class ThriftServer implements IkoalasServer {
    private final static Logger logger = LoggerFactory.getLogger ( ThriftServer.class );

    private AbstractKoalsServerPublisher serverPublisher;

    private TProcessor tProcessor;
    private TNonblockingServerSocket tServerTransport;
    private TServer tServer;

    private ExecutorService executorService;

    private ZookeeperServer zookeeperServer;


    public ThriftServer(AbstractKoalsServerPublisher serverPublisher) {
        this.serverPublisher = serverPublisher;
    }

    @Override
    public void run() {
         tProcessor = serverPublisher.getTProcessor ();
         if(tProcessor == null){
             logger.error ( "the tProcessor can't be null serverInfo={}",serverPublisher );
             throw new IllegalArgumentException("the tProcessor can't be null ");
         }
        try {
            tServerTransport = new  TNonblockingServerSocket (serverPublisher.port);
            KoalasThreadedSelectorServer.Args tArgs = new KoalasThreadedSelectorServer.Args(tServerTransport);
            TKoalasFramedTransport.Factory transportFactory = new TKoalasFramedTransport.Factory (  );
            TProtocolFactory tProtocolFactory = new TBinaryProtocol.Factory();
            tArgs.transportFactory(transportFactory);
            tArgs.protocolFactory(tProtocolFactory);
            tArgs.maxReadBufferBytes=2048000;
            tArgs.processor (tProcessor);
            tArgs.selectorThreads ( serverPublisher.bossThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS:serverPublisher.bossThreadCount );
            tArgs.workerThreads ( serverPublisher.workThreadCount==0? AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS*2:serverPublisher.workThreadCount);
            executorService = KoalasThreadedSelectorWorkerExcutorUtil.getWorkerExcutor (serverPublisher.koalasThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_KOALAS_THREADS:serverPublisher.koalasThreadCount,new KoalasDefaultThreadFactory (serverPublisher.serviceInterface.getName ()));
            tArgs.executorService (executorService);
            tArgs.acceptQueueSizePerThread(AbstractKoalsServerPublisher.DEFAULT_THRIFT_ACCETT_THREAD);
            tServer = new KoalasThreadedSelectorServer(tArgs);
            ((KoalasThreadedSelectorServer) tServer).setPrivateKey (serverPublisher.privateKey);
            ((KoalasThreadedSelectorServer) tServer).setPublicKey ( serverPublisher.publicKey );
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run(){
                    if(zookeeperServer != null){
                        zookeeperServer.destroy ();
                    }
                    logger.info ( "wait for service over 3000ms" );
                    try {
                        Thread.sleep ( 3000 );
                    } catch (InterruptedException e) {
                    }
                    if(tServer!= null && tServer.isServing ()){
                        tServer.stop ();
                    }
                    if(executorService!=null){
                        executorService.shutdown ();
                    }
                }
            });
            new Thread (new ThriftRunable(tServer) ).start ();

            if(StringUtils.isNotEmpty ( serverPublisher.zkpath )){
                ZookServerConfig zookServerConfig = new ZookServerConfig ( serverPublisher.zkpath,serverPublisher.serviceInterface.getName (),serverPublisher.env,serverPublisher.port,serverPublisher.weight,"thrift" );
                zookeeperServer = new ZookeeperServer ( zookServerConfig );
                zookeeperServer.init ();
            }

         } catch (TTransportException e) {
            logger.error ( "the tProcessor can't be null serverInfo={}",serverPublisher );
            throw new IllegalArgumentException("the tProcessor can't be null");
        }
        logger.info("thrift server init success server={}",serverPublisher);
    }

    @Override
    public void stop() {

        if(zookeeperServer != null){
            zookeeperServer.destroy ();
        }

        logger.info ( "wait for service over 3000ms" );
        try {
            Thread.sleep ( 3000 );
        } catch (InterruptedException e) {
        }

        if(tServer!= null && tServer.isServing ()){
            tServer.stop ();
        }

        if(executorService!=null){
            executorService.shutdown ();
        }

         logger.info("thrift server stop success server={}",serverPublisher);
    }

    private class ThriftRunable implements Runnable {

        private TServer tServer;

        public ThriftRunable(TServer tServer) {
            this.tServer = tServer;
        }

        @Override
        public void run() {
            tServer.serve ();
        }
    }
}
