package thrift;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.KoalasBinaryProtocol;
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
    private int stop =0;

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
            TProtocolFactory tProtocolFactory = new KoalasBinaryProtocol.Factory();
            tArgs.transportFactory(transportFactory);
            tArgs.protocolFactory(tProtocolFactory);
            tArgs.maxReadBufferBytes=serverPublisher.maxLength;
            tArgs.processor (tProcessor);
            tArgs.selectorThreads ( serverPublisher.bossThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS:serverPublisher.bossThreadCount );
            tArgs.workerThreads ( serverPublisher.workThreadCount==0? AbstractKoalsServerPublisher.DEFAULT_EVENT_LOOP_THREADS*2:serverPublisher.workThreadCount);
            executorService = KoalasThreadedSelectorWorkerExcutorUtil.getWorkerExcutor (serverPublisher.koalasThreadCount==0?AbstractKoalsServerPublisher.DEFAULT_KOALAS_THREADS:serverPublisher.koalasThreadCount,new KoalasDefaultThreadFactory (serverPublisher.serviceInterface.getName ()));
            tArgs.executorService (executorService);
            tArgs.acceptQueueSizePerThread(AbstractKoalsServerPublisher.DEFAULT_THRIFT_ACCETT_THREAD);
            tServer = new KoalasThreadedSelectorServer(tArgs);
            ((KoalasThreadedSelectorServer) tServer).setPrivateKey (serverPublisher.privateKey);
            ((KoalasThreadedSelectorServer) tServer).setPublicKey ( serverPublisher.publicKey );
            ((KoalasThreadedSelectorServer) tServer).setServiceName ( serverPublisher.getServiceInterface ().getName () );
            ((KoalasThreadedSelectorServer) tServer).settGenericProcessor ( serverPublisher.getGenericTProcessor () );
            ((KoalasThreadedSelectorServer) tServer).setCat ( serverPublisher.isCat () );
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run(){
                    stopThriftServer();
                }
            });
            new Thread (new ThriftRunnable(tServer) ).start ();

            if(StringUtils.isNotEmpty ( serverPublisher.zkpath )){
                ZookServerConfig zookServerConfig = new ZookServerConfig ( serverPublisher.zkpath,serverPublisher.serviceInterface.getName (),serverPublisher.env,serverPublisher.port,serverPublisher.weight,"thrift" );
                zookeeperServer = new ZookeeperServer ( zookServerConfig );
                zookeeperServer.init ();
            }

         } catch (TTransportException e) {
            logger.error ( "thrift server init fail service:" + serverPublisher.serviceInterface.getName (),e );
            stop();
            throw new IllegalArgumentException("thrift server init faid service:" + serverPublisher.serviceInterface.getName ());
        }
        logger.info("thrift server init success server={}",serverPublisher);
    }

    @Override
    public void stop() {
        stopThriftServer();
    }

    private void stopThriftServer(){
        if(stop == 0){
            stop = 1;
            logger.info("thrift server stop start server={}",serverPublisher);
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
            stop = 2;
            logger.info("thrift server stop success server={}",serverPublisher);
        }

        while (stop == 1){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("thrift server stop waiting serverPublisher:" + serverPublisher, e);
            }
        }

    }
    private class ThriftRunnable implements Runnable {

        private TServer tServer;

        public ThriftRunnable(TServer tServer) {
            this.tServer = tServer;
        }

        @Override
        public void run() {
            tServer.serve ();
        }
    }
}
