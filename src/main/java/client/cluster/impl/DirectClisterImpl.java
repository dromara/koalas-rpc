package client.cluster.impl;

import client.cluster.ILoadBalancer;
import client.cluster.RemoteServer;
import client.cluster.ServerObject;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (C) 22018
 * All rights reserved
 * User: yulong.zhang
 * Date: 2018年09月18日17:44:17
 */
public class DirectClisterImpl extends AbstractBaseIcluster {
    private static final Logger LOG = LoggerFactory.getLogger(DirectClisterImpl.class);
    public static final String REGEX = "[^0-9a-zA-Z_\\-\\.:#]+";

    //ip:port
    private String hostAndPorts;
    //负载策略，默认权重
    private ILoadBalancer iLoadBalancer;
    private String serviceName;
    //当前服务列表
    private List<RemoteServer> serverList = new ArrayList<> ();
    //服务资源连接池
    private ConcurrentHashMap<String,GenericObjectPool<TTransport>> serverPollMap = new ConcurrentHashMap<> (  );

    public DirectClisterImpl(String hostAndPorts, ILoadBalancer iLoadBalancer, String serviceName, boolean async, int conTimeOut, int soTimeOut, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig) {

        super(iLoadBalancer,serviceName,async,conTimeOut,soTimeOut,genericObjectPoolConfig,abandonedConfig);
        this.hostAndPorts = hostAndPorts;
        this.iLoadBalancer = iLoadBalancer;
        this.serviceName = serviceName;
    }

    @Override
    public RemoteServer getUseRemote() {
        if (serverList == null) {
            if (this.hostAndPorts == null) return null;
            String[] array = hostAndPorts.split ( REGEX );
            List<RemoteServer> list = new ArrayList<> ();
            for (String temp : array) {
                String hostAndIp = temp.split ( "#" )[0].trim ();
                Integer weight = Integer.valueOf ( temp.split ( "#" )[1].trim () );
                String host = hostAndIp.split ( ":" )[0].trim ();
                String port = hostAndIp.split ( ":" )[1].trim ();
                list.add ( new RemoteServer ( host, port, weight, true ) );
            }
            serverList =list;
        }
        return iLoadBalancer.select (serverList);
    }

    @Override
    public void destroy() {
        LOG.info ( "【{}】shut down",serviceName );
        serverList=null;//help gc
        if(serverPollMap !=null && serverPollMap.size ()>0){

            for(String string:serverPollMap.keySet ()){
                GenericObjectPool p =serverPollMap.get ( string );
                if(p!=null) p.close ();
            }
        }
    }

    @Override
    public ServerObject getObjectForRemote() {
        RemoteServer remoteServer= this.getUseRemote();
        if(serverPollMap.containsKey ( createMapKey(remoteServer) )){
            GenericObjectPool<TTransport> pool = serverPollMap.get ( createMapKey(remoteServer) );
            try {
                return createServerObject(pool,remoteServer);
            } catch (Exception e) {
                LOG.error ( "borrowObject is wrong,the poll message is:",e );
                return null;
            }
        }

        GenericObjectPool pool = createGenericObjectPool(remoteServer);
        serverPollMap.put (createMapKey(remoteServer) ,pool);
        try {
            return createServerObject(pool,remoteServer);
        } catch (Exception e) {
            LOG.error ( "borrowObject is wrong,the poll message is:",e );
            return null;
        }
    }

    private ServerObject createServerObject(GenericObjectPool pool,RemoteServer remoteServer){
        ServerObject serverObject = new ServerObject ();
        serverObject.setGenericObjectPool ( pool );
        serverObject.setRemoteServer ( remoteServer );
        return serverObject;
    }

    private String createMapKey(RemoteServer remoteServer){
        return remoteServer.getIp ().concat ( "-" ).concat (remoteServer.getPort ());
    }

    @Override
    public void updateRemoteServer(RemoteServer list) {
        //do nothing
    }

}
