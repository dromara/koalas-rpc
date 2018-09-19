package client.cluster;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.transport.TTransport;

public class ServerObject {

    private GenericObjectPool<TTransport> genericObjectPool;
    private RemoteServer remoteServer;

    public GenericObjectPool<TTransport> getGenericObjectPool() {
        return genericObjectPool;
    }

    public void setGenericObjectPool(GenericObjectPool<TTransport> genericObjectPool) {
        this.genericObjectPool = genericObjectPool;
    }

    public RemoteServer getRemoteServer() {
        return remoteServer;
    }

    public void setRemoteServer(RemoteServer remoteServer) {
        this.remoteServer = remoteServer;
    }
}
