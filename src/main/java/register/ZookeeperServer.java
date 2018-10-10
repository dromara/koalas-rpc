package register;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.config.ZookServerConfig;
import utils.IPUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;

public class ZookeeperServer {
    private static final Logger LOG = LoggerFactory.getLogger ( ZookeeperServer.class );
    public static final int RETRY_TIMES = 2;
    public static final int SESSION_TIMEOUT = 3000;
    public static final String UTF_8 = "UTF-8";
    private ZookServerConfig zookServerConfig;
    private ZooKeeper zookeeper = null;

    public ZookeeperServer(ZookServerConfig zookServerConfig) {
        if (zookServerConfig == null) throw new IllegalArgumentException ( "zookServerConfig can't be null" );
        this.zookServerConfig = zookServerConfig;
    }

    public void init() {

        String env = zookServerConfig.getEnv ();
        String service = zookServerConfig.getService ();
        int port = zookServerConfig.getPort ();
        int weight = zookServerConfig.getWeight ();
        String zkpath = zookServerConfig.getZkpath ();

        CountDownLatch c = new CountDownLatch ( 1 );
        if (zookeeper == null) {
            int retry = 0;
            //网络抖动重试3次
            while (retry++ < RETRY_TIMES) {
                try {
                    zookeeper = new ZooKeeper ( zkpath, SESSION_TIMEOUT, new koalasWatcher ( c ) );
                    break;
                } catch (IOException e) {
                    LOG.error ( "zk server faild service:" + env + "-" + service, e );
                }
            }
        }

        if (zookeeper == null) {
            LOG.error ( "zk server is null service:" + env + "-" + service );
            throw new IllegalArgumentException ( "zk server can't be null" );
        }

        try {
            String envPath = env.startsWith ( "/" ) ? env : "/".concat ( env );
            c.await ();
            if (zookeeper.exists ( envPath, null ) == null) {
                zookeeper.create ( envPath, "".getBytes (), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
            }
            String servicePath = service.startsWith ( "/" ) ? service : "/".concat ( service );

            if (zookeeper.exists ( envPath + servicePath, null ) == null) {
                zookeeper.create ( envPath + servicePath, "".getBytes (), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT );
            }

            JSONObject jsonChildData = new JSONObject ();
            jsonChildData.put ( "weight", weight == 0 ? 10 : weight );
            jsonChildData.put ( "enable", 1 );

            String ip = IPUtil.getIpV4 ();
            if (StringUtils.isEmpty ( ip )) {
                throw new IllegalArgumentException ( "ip can't be null" );
            }
            String childPathData = jsonChildData.toJSONString ();

            String childpath;
            if (zookeeper.exists ( childpath = (envPath + servicePath + "/" + ip + ":" + port), null ) == null) {
                zookeeper.create ( childpath, childPathData.getBytes ( UTF_8 ), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL );
            }
            LOG.info ( "zk server init success,info={}", zookServerConfig );
        } catch (KeeperException e) {
            e.printStackTrace ();
            LOG.error ( e.getMessage (), e );
        } catch (InterruptedException e) {
            LOG.error ( e.getMessage (), e );
        } catch (UnsupportedEncodingException e) {
            LOG.error ( e.getMessage (), e );
        }
    }

    public void destroy() {
        if (zookeeper != null) {
            try {
                zookeeper.close ();
            } catch (InterruptedException e) {
                LOG.error ( "the service 【{}】zk close faild info={}", zookServerConfig );
            }
        }
    }

    private class koalasWatcher implements Watcher {

        private CountDownLatch countDownLatch;

        public koalasWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void process(WatchedEvent event) {
            if (Event.KeeperState.SyncConnected == event.getState ()) {
                countDownLatch.countDown ();
            }
        }
    }

}
