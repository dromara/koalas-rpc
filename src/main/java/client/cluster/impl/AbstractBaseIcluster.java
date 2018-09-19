package client.cluster.impl;

import client.cluster.ILoadBalancer;
import client.cluster.Icluster;
import client.cluster.RemoteServer;
import client.cluster.ServerObject;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poolfactory.KoalasPoolableObjectFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 22018
 * All rights reserved
 * User: yulong.zhang
 * Date: 2018年09月18日17:43:57
 */
public abstract class AbstractBaseIcluster implements Icluster {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseIcluster.class);

    private ILoadBalancer iLoadBalancer;
    private String serviceName;
    private boolean async;
    private int conTimeOut;
    private int soTimeOut;
    private GenericObjectPoolConfig genericObjectPoolConfig;
    private AbandonedConfig abandonedConfig;

    public AbstractBaseIcluster(ILoadBalancer iLoadBalancer, String serviceName, boolean async, int conTimeOut, int soTimeOut, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig) {
        this.iLoadBalancer = iLoadBalancer;
        this.serviceName = serviceName;
        this.async = async;
        this.conTimeOut = conTimeOut;
        this.soTimeOut = soTimeOut;
        this.genericObjectPoolConfig = genericObjectPoolConfig;
        this.abandonedConfig = abandonedConfig;
    }


    protected GenericObjectPool createGenericObjectPool(RemoteServer remoteServer) {
        GenericObjectPool<TTransport> genericObjectPool = new GenericObjectPool ( new KoalasPoolableObjectFactory (remoteServer, this.conTimeOut, this.soTimeOut, this.async ), this.genericObjectPoolConfig );
        genericObjectPool.setAbandonedConfig ( this.abandonedConfig );
        if (genericObjectPoolConfig.getMinIdle () == 0) {
            genericObjectPool.setMinEvictableIdleTimeMillis ( -1 );
            genericObjectPool.setSoftMinEvictableIdleTimeMillis ( -1 );
        }
        return genericObjectPool;
    }

    protected void destroyGenericObjectPool(GenericObjectPool genericObjectPool) {
         if(genericObjectPool != null){
             genericObjectPool.close ();
         }
    }

    //服务销毁
    public abstract void destroy();

    //获取TCP远程连接资源
    public abstract ServerObject getObjectForRemote();

}