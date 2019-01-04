package register;

import client.cluster.RemoteServer;
import client.cluster.impl.ZookeeperClisterImpl;
import com.alibaba.fastjson.JSONObject;
import heartbeat.request.HeartBeat;
import heartbeat.service.HeartbeatService;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transport.TKoalasFramedTransport;
import utils.IPUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class ZookeeperClient {
    private static final Logger LOG = LoggerFactory.getLogger ( ZookeeperClient.class );
    public static final int RETRY_TIMES = 2;
    public static final int SESSION_TIMEOUT = 3000;
    public static final Watcher NULL = null;
    public static final String UTF_8 = "UTF-8";
    public static final int TIMEOUT = 3000;
    public static final byte HEARTBEAT = (byte) 2;
    private String env;
    private String path;
    private String serviceName;
    private ZooKeeper zookeeper = null;
    private ZookeeperClisterImpl zookeeperClister;
    //private Map<String, Watcher> serviceWatcher = new ConcurrentHashMap<> ();
    private CountDownLatch firstInitChildren = new CountDownLatch(1);
    //private boolean firstInitChildren = true;

    //当前服务列表
    private List<RemoteServer> serverList = new CopyOnWriteArrayList<> ();

    //心跳服务列表
    private Map<String, HeartbeatService.Client> serverHeartbeatMap = new ConcurrentHashMap<> ();

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool ( 1 );

    public List<RemoteServer> getServerList() {
        return serverList;
    }

    public ZookeeperClient(String env, String path, String serviceName, ZookeeperClisterImpl zookeeperClister) {
        if (env == null) {
            throw new RuntimeException ( "env can't be null" );
        }
        if (serviceName == null) {
            throw new RuntimeException ( "serviceName can't be null" );
        }
        if (path == null) {
            throw new RuntimeException ( "zk ip and port can't be null" );
        }
        this.env = env;
        this.path = path;
        this.serviceName = serviceName;
        this.zookeeperClister = zookeeperClister;
    }

    public void initZooKeeper() {

        CountDownLatch c = new CountDownLatch ( 1 );

        if (zookeeper == null) {
            try {
                zookeeper = new ZooKeeper ( path, SESSION_TIMEOUT, new ClinetInitWatcher ( c ) );
            } catch (IOException e) {
                LOG.error ( "zk server faild service:" + env + "-" + serviceName, e );
            }
        }

        try {
            //网络抖动重试3次
            int retry = 0;
            boolean connected = false;
            while (retry++ < RETRY_TIMES) {
                if (c.await ( 5, TimeUnit.SECONDS )) {
                    connected = true;
                    break;
                }
            }
            if (!connected) {
                LOG.error ( "zk client connected fail! :" + env + "-" + serviceName );
                throw new IllegalArgumentException ( "zk client connected fail!" );
            }
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        }

        try {
            String envPath = env.startsWith ( "/" ) ? env : "/".concat ( env );
            if (zookeeper.exists ( envPath, null ) == null) {
                zookeeper.create ( envPath, "".getBytes (), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
            }
            String servicePath = serviceName.startsWith ( "/" ) ? serviceName : "/".concat ( serviceName );
            if (zookeeper.exists ( envPath + servicePath, null ) == null) {
                zookeeper.create ( envPath + servicePath, "".getBytes (), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
            }

            Watcher w = new koalasWatcher ();
            //    /env/com.test.service
            getChildren ( envPath + servicePath, w );
            watchChildDateChange ( envPath + servicePath, w );
            initScheduled ();
        } catch (KeeperException e) {
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        }
    }

    private void initScheduled() {

        Calendar now = Calendar.getInstance ();
        Calendar after = Calendar.getInstance ();
        after.set ( Calendar.MINUTE, now.get ( Calendar.MINUTE ) + 1 );
        after.set ( Calendar.SECOND, 0 );
        after.set ( Calendar.MILLISECOND, 0 );
         if((after.getTimeInMillis ()-now.getTimeInMillis ())>= 10*1000){
             executor.scheduleWithFixedDelay ( new HeartbeatRun(),(after.getTimeInMillis ()-now.getTimeInMillis ())/1000,60,TimeUnit.SECONDS );
         } else{
             executor.scheduleWithFixedDelay ( new HeartbeatRun(),(after.getTimeInMillis ()-now.getTimeInMillis ())/1000 +  60,60,TimeUnit.SECONDS );
         }
    }

    private void watchChildDateChange(String path, Watcher w) {
        //path /env/com.test.service
        try {
            //192.168.3.1:6666 192.168.3.1:9990 192.168.3.1:9999
            List<String> childpaths = this.zookeeper.getChildren ( path, null );

            for (String _childPath : childpaths) {
                //  /env/com.test.service/192.168.3.2:8080
                String fullPath = path.concat ( "/" ).concat ( _childPath );
                this.zookeeper.getData ( fullPath, w, new Stat () );
            }

        } catch (KeeperException e) {
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        } finally {
            firstInitChildren.countDown ();
        }
    }

    private void getChildren(String path, Watcher w) {
        //path /env/com.test.service
        try {
            //192.168.3.1:6666 192.168.3.1:9990 192.168.3.1:9999
            List<String> childpaths = this.zookeeper.getChildren ( path, w );
            updateServerList ( childpaths, path );
        } catch (KeeperException e) {
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        }
    }

    private class koalasWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            if (event.getState () == Event.KeeperState.SyncConnected) {
                //when the new server or shutdown
                if (event.getType () == Event.EventType.NodeChildrenChanged) {
                    //  /env/com.test.service
                    String parentPath = event.getPath ();
                    LOG.info ( "the service {} is changed ! ", serviceName );
                    try {
                        //wait the init childChanged
                        firstInitChildren.await ();

                        List<String> childpaths = ZookeeperClient.this.zookeeper.getChildren ( parentPath, this );
                        ZookeeperClient.this.updateServerList ( childpaths, parentPath );
                        LOG.info ( "the serviceList: {} ! ", childpaths );

                        for (String _childpaths : childpaths) {
                            String fullpath = parentPath.concat ( "/" ).concat ( _childpaths );
                            //192.168.3.1
                            ZookeeperClient.this.zookeeper.getData ( fullpath, this, new Stat () );
                        }

                    } catch (KeeperException e) {
                        LOG.error ( e.getMessage (), e );
                    } catch (InterruptedException e) {
                        LOG.error ( e.getMessage (), e );
                    }
                }

                if (event.getType () == Event.EventType.NodeDataChanged) {
                    //  /env/com.test.service/192.168.3.2:6666
                    String fullPath = event.getPath ();
                    LOG.info ( "the service 【{}】 data {} is changed ! full mess is 【{}】 ", serviceName, fullPath );

                    try {
                        //wait the init childDataChanged
                        firstInitChildren.await ();
                        String data = new String ( ZookeeperClient.this.zookeeper.getData ( fullPath, this, new Stat () ), UTF_8 );
                        JSONObject json = JSONObject.parseObject ( data );

                        //192.168.3.2:6666
                        String _childPath = fullPath.substring ( fullPath.lastIndexOf ( "/" ) + 1 );
                        //远程ip
                        String ip = _childPath.split ( ":" )[0];
                        //服务端口
                        String port = _childPath.split ( ":" )[1];
                        //权重
                        String weight = json.getString ( "weight" );
                        //是否可用
                        String enable = json.getString ( "enable" );
                        //注冊类型
                        String server = json.getString ( "server" );

                        RemoteServer remoteServer = new RemoteServer ( ip, port, Integer.valueOf ( weight ), "1".equals ( enable ), server );
                        ZookeeperClient.this.updateServer ( remoteServer );


                    } catch (KeeperException e) {
                        e.printStackTrace ();
                    } catch (InterruptedException e) {
                        e.printStackTrace ();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace ();
                    }


                }
            }
        }
    }

    private  void updateServer(RemoteServer remoteServer) {
       try {
           zookeeperClister.writeLock.lock();
           if (serverList != null)
               for (int i = 0; i < serverList.size (); i++) {
                   RemoteServer tempServer_ = serverList.get ( i );
                   if (tempServer_.getIp ().equals ( remoteServer.getIp () ) && tempServer_.getPort ().equals ( remoteServer.getPort () )) {
                       serverList.set ( i, remoteServer );
                       tempServer_ = null; //help gc
                   }
               }
       } finally {
           zookeeperClister.writeLock.unlock();
       }
    }

    private  void updateServerList(List<String> childpaths, String parentPath) {

        try {
            zookeeperClister.writeLock.lock();

            if (serverList.size () != 0) {
                serverList.clear ();
            }
            if (serverHeartbeatMap.size () != 0) {
                serverHeartbeatMap.clear ();
            }
            if (zookeeperClister.serverPollMap != null && zookeeperClister.serverPollMap.size () > 0) {
                for (String string : zookeeperClister.serverPollMap.keySet ()) {
                    GenericObjectPool p = zookeeperClister.serverPollMap.get ( string );
                    if (p != null) p.close ();
                    zookeeperClister.serverPollMap.remove ( string );
                }
            }

            for (String _childPath : childpaths) {
                //   /env/com.test.service/192.168.1.10:6666
                String currPath = parentPath.concat ( "/" ).concat ( _childPath );
                //
                try {
                    byte[] bytes = zookeeper.getData ( currPath, null, new Stat () );
                    try {
                        String data = new String ( bytes, "UTF-8" );
                        JSONObject json = JSONObject.parseObject ( data );

                        //远程ip
                        String ip = _childPath.split ( ":" )[0];
                        //服务端口
                        String port = _childPath.split ( ":" )[1];
                        //权重
                        String weight = json.getString ( "weight" );
                        //是否可用
                        String enable = json.getString ( "enable" );
                        //注冊类型
                        String server = json.getString ( "server" );
                        RemoteServer remoteServer = new RemoteServer ( ip, port, Integer.valueOf ( weight ), "1".equals ( enable ), server );
                        serverList.add ( remoteServer );

                        //Heartbeat
                        TSocket t = new TSocket ( remoteServer.getIp (), Integer.parseInt ( remoteServer.getPort () ), TIMEOUT );
                        TTransport transport = new TKoalasFramedTransport ( t );
                        ((TKoalasFramedTransport) transport).setHeartbeat ( HEARTBEAT );
                        TProtocol protocol = new TBinaryProtocol ( transport );
                        HeartbeatService.Client client = new HeartbeatService.Client ( protocol );
                        transport.open ();
                        serverHeartbeatMap.put ( zookeeperClister.createMapKey ( remoteServer ), client );

                    } catch (UnsupportedEncodingException e) {
                        LOG.error ( e.getMessage () + " UTF-8 is not allow!", e );
                    } catch (TTransportException e) {
                        LOG.error ( e.getMessage (), e );
                    }
                } catch (KeeperException e) {
                    LOG.error ( e.getMessage () + "currPath is not exists!", e );
                } catch (InterruptedException e) {
                    LOG.error ( e.getMessage () + "the current thread is Interrupted", e );
                }
            }
        } finally {
            zookeeperClister.writeLock.unlock();
        }

    }


    private class ClinetInitWatcher implements Watcher {

        private CountDownLatch countDownLatch;

        public ClinetInitWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void process(WatchedEvent event) {
            if (Event.KeeperState.SyncConnected == event.getState ()) {
                LOG.info ( "the service {}-{}-{} is SyncConnected!", IPUtil.getIpV4 (), ZookeeperClient.this.env, ZookeeperClient.this.serviceName );
                countDownLatch.countDown ();
            }
            if (Event.KeeperState.Expired == event.getState ()) {
                LOG.warn ( "the service {}-{}-{} is expired!", IPUtil.getIpV4 (), ZookeeperClient.this.env, ZookeeperClient.this.serviceName );
                reConnected ();
            }
            if (Event.KeeperState.Disconnected == event.getState ()) {
                LOG.warn ( "the service {}-{}-{} is Disconnected!", IPUtil.getIpV4 (), ZookeeperClient.this.env, ZookeeperClient.this.serviceName );
            }
        }

        private  void reConnected() {
            try {
                zookeeperClister.writeLock.lock();
                ZookeeperClient.this.destroy ();
                firstInitChildren = new CountDownLatch ( 1 );
                serverList = new CopyOnWriteArrayList<> ();
                //心跳服务列表
                serverHeartbeatMap = new ConcurrentHashMap<> ();
                executor = Executors.newScheduledThreadPool ( 1 );
                ZookeeperClient.this.initZooKeeper ();
            } finally {
                zookeeperClister.writeLock.unlock();
            }
        }
    }

    public void destroy() {
        serverList = null; //help gc
        if (!executor.isShutdown ()) {
            executor.shutdownNow ();
            executor = null;
        }

        serverHeartbeatMap = null;

        if (zookeeper != null) {
            try {
                zookeeper.close ();
                zookeeper = null;
            } catch (InterruptedException e) {
                LOG.error ( "the service 【{}】zk close faild", env.concat ( serviceName ) );
            }
        }

    }

    private class HeartbeatRun implements Runnable {

        @Override
        public void run() {
            try {
                zookeeperClister.writeLock.lock();
                if (serverHeartbeatMap != null && serverHeartbeatMap.size () > 0) {
                    Iterator<String> key = serverHeartbeatMap.keySet ().iterator ();
                    in:
                    while (key.hasNext ()) {
                        String str = key.next ();
                        String ip = str.split ( "-" )[0];
                        String port = str.split ( "-" )[1];

                        if (serverList != null) {
                            for(int i=serverList.size ()-1;i>=0;i--){
                                RemoteServer remoteServer =serverList.get ( i );
                                if (remoteServer.getIp ().equals ( ip ) && remoteServer.getPort ().equals ( port )) {
                                    if(!remoteServer.isEnable ()){
                                        continue  in;
                                    }
                                }
                            }
                        }

                        HeartbeatService.Client client = serverHeartbeatMap.get ( str );
                        HeartBeat heartBeat = new HeartBeat ();
                        heartBeat.setIp ( IPUtil.getIpV4 () );
                        heartBeat.setServiceName ( ZookeeperClient.this.serviceName );
                        heartBeat.setDate ( new SimpleDateFormat ( "yyyy-MM-dd hh:mm:ss" ).format ( new Date () ) );
                        int retry = 3;
                        while (retry-- > 0) {
                            try {
                                HeartBeat respone = client.getHeartBeat ( heartBeat );
                                LOG.info ( "HeartBeat info:ip:{},serviceName:{}", respone.getIp (), serviceName );
                                break;
                            } catch (Exception e) {
                                if (retry == 0) {
                                    LOG.warn ( "HeartBeat error:{}", heartBeat );

                                    if (serverList != null) {
                                        for(int i=serverList.size ()-1;i>=0;i--){
                                            RemoteServer remoteServer =serverList.get ( i );
                                            if (remoteServer.getIp ().equals ( ip ) && remoteServer.getPort ().equals ( port )) {
                                                try {
                                                    serverList.remove ( i );
                                                    key.remove ();
                                                    if(zookeeperClister.serverPollMap.containsKey (str)){
                                                        GenericObjectPool<TTransport> transport = zookeeperClister.serverPollMap.get (  str);
                                                        transport.close ();
                                                    }
                                                    continue in;
                                                }catch (Exception e1){
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } finally {
                zookeeperClister.writeLock.unlock();
            }
        }
    }
}
