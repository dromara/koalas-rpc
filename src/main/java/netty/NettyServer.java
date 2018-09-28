package netty;

import server.IkoalasServer;
import server.config.AbstractKoalsServerPublisher;

public class NettyServer implements IkoalasServer {

    private AbstractKoalsServerPublisher serverPublisher;

    public NettyServer(AbstractKoalsServerPublisher serverPublisher) {
        this.serverPublisher = serverPublisher;
    }

    @Override
    public void run() {

    }

    @Override
    public void stop() {

    }
}
