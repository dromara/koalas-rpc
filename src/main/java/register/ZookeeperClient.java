package register;

import client.cluster.RemoteServer;
import client.cluster.impl.ZookeeperClisterImpl;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ZookeeperClient {
    private static final Logger LOG = LoggerFactory.getLogger ( ZookeeperClient.class );
    public static final int RETRY_TIMES = 2;
    public static final int SESSION_TIMEOUT = 3000;
    public static final Watcher NULL = null;
    public static final String UTF_8 = "UTF-8";
    private String env;
    private String path;
    private String serviceName;
    private ZooKeeper zookeeper = null;
    private ZookeeperClisterImpl zookeeperClister;

    private Map<String, Watcher> serviceWatcher = new ConcurrentHashMap<> ();

    private boolean firstInitChildren = true;

    //当前服务列表
    private List<RemoteServer> serverList = new CopyOnWriteArrayList<> ();

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
        if (zookeeper == null) {
            int retry = 0;
            //网络抖动重试3次
            while (retry++ > RETRY_TIMES) {
                try {
                    zookeeper = new ZooKeeper ( path, SESSION_TIMEOUT, NULL );
                    break;
                } catch (IOException e) {
                    LOG.error ( "zk server faild service:" + env + "-" + serviceName, e );
                }
            }
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

            //    /env/com.test.service
            getChildren ( envPath + servicePath );
            watchChildDateChange ( envPath + servicePath );

        } catch (KeeperException e) {
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        }
    }

    public void watchChildDateChange(String path) {
        //path /env/com.test.service
        try {
            //192.168.3.1:6666 192.168.3.1:9990 192.168.3.1:9999
            List<String> childpaths = this.zookeeper.getChildren ( path, null );

            for (String _childPath : childpaths) {
                //  /env/com.test.service/192.168.3.2:8080
                String fullPath = path.concat ( "/" ).concat ( _childPath );

                //防止服务多次重启引起的多次监听,使用同一个Watcher会避免这个问题
                Watcher w = new koalasWatcher ();
                serviceWatcher.put ( _childPath, w );

                this.zookeeper.getData ( fullPath, w, new Stat () );
            }


        } catch (KeeperException e) {
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        } finally {
            firstInitChildren = false;
        }
    }

    public void getChildren(String path) {
        //path /env/com.test.service
        try {
            //192.168.3.1:6666 192.168.3.1:9990 192.168.3.1:9999
            List<String> childpaths = this.zookeeper.getChildren ( path, new koalasWatcher () );
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
                    LOG.info ( "the service { } is changed ! ", serviceName );
                    try {
                        List<String> childpaths = ZookeeperClient.this.zookeeper.getChildren ( parentPath, this );
                        ZookeeperClient.this.updateServerList ( childpaths, parentPath );

                        //wait the init childChanged
                        while (firstInitChildren) {
                            Thread.sleep ( 10l );
                        }

                        for (String _childpaths : childpaths) {
                            //192.168.3.1
                            if (!serviceWatcher.containsKey ( _childpaths )) {
                                ZookeeperClient.this.zookeeper.getData ( parentPath.concat ( "/" ).concat ( _childpaths ), this, new Stat () );
                                serviceWatcher.put ( _childpaths, this );
                            }
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
                    LOG.info ( "the service 【{}】 data { } is changed ! full mess is 【{}】 ", serviceName, fullPath );

                    try {
                        //wait the init childDataChanged
                        while (firstInitChildren) {
                            Thread.sleep ( 10l );
                        }
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

                        RemoteServer remoteServer = new RemoteServer ( ip, port, Integer.valueOf ( weight ), "1".equals ( enable ) );
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

    private synchronized void updateServer(RemoteServer remoteServer) {
        if (serverList != null)
            for (int i = 0; i < serverList.size (); i++) {
                RemoteServer tempServer_ = serverList.get ( i );
                if (tempServer_.getIp ().equals ( remoteServer.getIp () ) && remoteServer.getPort ().equals ( remoteServer.getPort () )) {
                    serverList.set ( i, remoteServer );
                    tempServer_ = null; //help gc
                }
            }
    }

    private synchronized void updateServerList(List<String> childpaths, String parentPath) {
        if (serverList.size () != 0) {
            serverList.clear ();
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

                    RemoteServer remoteServer = new RemoteServer ( ip, port, Integer.valueOf ( weight ), "1".equals ( enable ) );
                    serverList.add ( remoteServer );

                } catch (UnsupportedEncodingException e) {
                    LOG.error ( e.getMessage () + " UTF-8 is not allow!", e );
                }
            } catch (KeeperException e) {
                LOG.error ( e.getMessage () + "currPath is not exists!", e );
            } catch (InterruptedException e) {
                LOG.error ( e.getMessage () + "the current thread is Interrupted", e );
            }
        }
    }

    public void destroy() {
        serverList = null; //help gc
        if (zookeeper != null) {
            try {
                zookeeper.close ();
            } catch (InterruptedException e) {
                LOG.error ( "the service 【{}】zk close faild", env.concat ( serviceName ) );
            }
        }

    }

}
