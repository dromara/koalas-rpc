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
import register.ZookeeperClient;

public class ZookeeperClisterImpl extends AbstractBaseIcluster {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperClisterImpl.class);

    private String hostAndPorts;
    private String env;
    //负载策略，默认权重
    private ILoadBalancer iLoadBalancer;
    private String serviceName;
    private ZookeeperClient zookeeperClient;


    public ZookeeperClisterImpl(String hostAndPorts, ILoadBalancer iLoadBalancer, String serviceName,String env, boolean async, int conTimeOut, int soTimeOut, GenericObjectPoolConfig genericObjectPoolConfig, AbandonedConfig abandonedConfig){
        super(iLoadBalancer,serviceName,async,conTimeOut,soTimeOut,genericObjectPoolConfig,abandonedConfig);
        this.hostAndPorts=hostAndPorts;
        this.iLoadBalancer=iLoadBalancer;
        this.serviceName=serviceName;
        this.env=env;
        initZKclient (this.hostAndPorts,this.serviceName,this.env);
    }

    @Override
    public RemoteServer getUseRemote() {
        synchronized (zookeeperClient){
            return iLoadBalancer.select (zookeeperClient.getServerList ());
        }
    }

    @Override
    public void destroy() {
        LOG.info ( "【{}】shut down",serviceName );
        if(serverPollMap !=null && serverPollMap.size ()>0){
            for(String string:serverPollMap.keySet ()){
                GenericObjectPool p =serverPollMap.get ( string );
                if(p!=null) destroyGenericObjectPool(p);
                serverPollMap.remove (  string);
            }
        }
        if(zookeeperClient != null)
            synchronized (zookeeperClient){
                zookeeperClient.destroy ();
            }
    }

    @Override
    public ServerObject getObjectForRemote() {
        RemoteServer remoteServer= this.getUseRemote();
        if(remoteServer==null){
            LOG.error ( "there is no server list serviceName={},hostAndPorts={},env={}",serviceName,hostAndPorts,env);
            return null;
        }
        if(serverPollMap.containsKey ( createMapKey(remoteServer) )){
            GenericObjectPool<TTransport> pool = serverPollMap.get ( createMapKey(remoteServer) );
            try {
                return createServerObject(pool,remoteServer);
            } catch (Exception e) {
                LOG.error ( "borrowObject is wrong,the poll message is:",e );
                return null;
            }
        }

        GenericObjectPool<TTransport> pool = createGenericObjectPool(remoteServer);
        serverPollMap.put (createMapKey(remoteServer) ,pool);
        try {
            return createServerObject(pool,remoteServer);
        } catch (Exception e) {
            LOG.error ( "borrowObject is wrong,the poll message is:",e );
            return null;
        }
    }

    private void initZKclient(String hostAndPorts,String serviceName,String env){
        if(zookeeperClient==null){
            zookeeperClient = new ZookeeperClient ( env,hostAndPorts, serviceName,this);
            zookeeperClient.initZooKeeper ();
        }
    }

    private ServerObject createServerObject(GenericObjectPool<TTransport> pool,RemoteServer remoteServer){
        ServerObject serverObject = new ServerObject ();
        serverObject.setGenericObjectPool ( pool );
        serverObject.setRemoteServer ( remoteServer );
        return serverObject;
    }

    private String createMapKey(RemoteServer remoteServer){
        return remoteServer.getIp ().concat ( "-" ).concat (remoteServer.getPort ());
    }

}
