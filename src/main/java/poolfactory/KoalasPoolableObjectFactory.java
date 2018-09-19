package poolfactory;

import client.cluster.RemoteServer;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;

/**
 * Copyright (C) 22018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:45:05
 */
public class KoalasPoolableObjectFactory extends BasePooledObjectFactory<TTransport> {

    private static final Logger LOG = LoggerFactory.getLogger(KoalasPoolableObjectFactory.class);

    private RemoteServer remoteServer;
    private int timeOut;
    private int connTimeOut;
    private boolean async;

    public KoalasPoolableObjectFactory(RemoteServer remoteServer, int timeOut, int connTimeOut, boolean async) {
        this.remoteServer = remoteServer;
        this.timeOut = timeOut;
        this.connTimeOut = connTimeOut;
        this.async = async;
    }

    @Override
    public TTransport create() throws Exception {

        //申请资源做三次重试
        int count = 3;
        TTransportException exception = null;
        while (count-- > 0) {
            long start = System.currentTimeMillis();
            TTransport transport = null;
            boolean connectSuccess = false;
            try {
                if(async) {//异步
                    transport = new TNonblockingSocket (remoteServer.getIp (), Integer.valueOf ( remoteServer.getPort () ), this.connTimeOut);
                    LOG.debug(
                            "makeObject() " + ((TNonblockingSocket) transport)
                                    .getSocketChannel().socket());
                } else {//同步
                    transport = new TSocket (remoteServer.getIp (), Integer.valueOf ( remoteServer.getPort () ),
                            this.connTimeOut);
                    transport.open();
                    ((TSocket)transport).setTimeout(timeOut);
                    LOG.debug(
                            "makeObject() " + ((TSocket) transport).getSocket());
                }
                connectSuccess = true;
                return transport;
            } catch (TTransportException te) {
                exception = te;
                LOG.warn(new StringBuilder("makeObject() ").append(te.getLocalizedMessage()).append(":").append(te.getType()).append("/")
                        .append(remoteServer.getIp ()).append(":").append(remoteServer.getPort ()).append("/").append(System.currentTimeMillis() - start).toString());
                // 连接超时时返回SocketTimeoutException
                if (!(te.getCause() instanceof SocketTimeoutException))
                    break;
            } catch (Exception e) {
                LOG.warn("makeObject()", e);
                throw new RuntimeException(e);
            } finally {
                if (transport != null && connectSuccess == false) {
                    try {
                        transport.close();
                    } catch (Exception e) {
                        LOG.warn(e.getMessage(), e);
                    }
                }
            }
        }
        throw new RuntimeException(exception);
    }

    @Override
    public PooledObject wrap(TTransport obj) {
        return new DefaultPooledObject<> ( obj );
    }

    @Override
    public void destroyObject(PooledObject<TTransport> p){

        TTransport tTransport = p.getObject ();
        if (tTransport instanceof TSocket) {
            TSocket socket = (TSocket) tTransport;
            if (socket.isOpen()) {
                LOG.debug("destroyObject() host:" + remoteServer.getIp () + ",port:" + remoteServer.getPort ()
                        + ",socket:" + socket.getSocket() + ",isOpen:" + socket
                        .isOpen());
                socket.close();
            }
        } else if(tTransport instanceof TNonblockingSocket) {
            TNonblockingSocket socket = (TNonblockingSocket) tTransport;
            if (socket.getSocketChannel().isOpen()) {
                LOG.debug("destroyObject() host:" + remoteServer.getIp () + ",port:" + remoteServer.getPort ()
                        + ",isOpen:" + socket.isOpen());
                socket.close();
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<TTransport> p)
    {
        TTransport tTransport = p.getObject ();
        try {
            if (tTransport instanceof TSocket) {
                TSocket thriftSocket = (TSocket) tTransport;
                if (thriftSocket.isOpen()) {
                    return true;
                } else {
                    LOG.warn("validateObject() failed " + thriftSocket.getSocket());
                    return false;
                }
            } else if(tTransport instanceof TNonblockingSocket) {
                TNonblockingSocket socket = (TNonblockingSocket) tTransport;
                if (socket.getSocketChannel().isOpen()) {
                    return true;
                } else {
                    LOG.warn("validateObject() failed " + socket.getSocketChannel().socket());
                    return false;
                }
            } else {
                LOG.warn("validateObject() failed unkown Object:" + tTransport);
                return false;
            }
        } catch (Exception e) {
            LOG.warn("validateObject() failed " + e.getLocalizedMessage());
            return false;
        }
    }

    /**
     *  No-op.
     *
     *  @param p ignored
     */
    @Override
    public void activateObject(PooledObject<TTransport> p) throws Exception {
        LOG.info ( "activateObject:PooledObject:【{}】",p );
    }

    /**
     *  No-op.
     *
     * @param p ignored
     */
    @Override
    public void passivateObject(PooledObject<TTransport> p)
            throws Exception {
        LOG.info ( "passivateObject:PooledObject:【{}】",p );
    }
}
