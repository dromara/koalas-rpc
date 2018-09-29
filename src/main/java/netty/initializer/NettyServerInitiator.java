package netty.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import netty.hanlder.KoalasDecoder;
import netty.hanlder.KoalasEncoder;
import netty.hanlder.KoalasHandler;
import org.apache.thrift.TProcessor;

import java.util.concurrent.ExecutorService;

public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private TProcessor tProcessor;

    private ExecutorService executorService;

    public NettyServerInitiator(TProcessor tProcessor, ExecutorService executorService) {
        this.tProcessor = tProcessor;
        this.executorService = executorService;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline ().addLast ( "decoder",new KoalasDecoder () );
        ch.pipeline ().addLast ( "encoder",new KoalasEncoder ());
        ch.pipeline ().addLast ( "handler",new KoalasHandler (tProcessor,executorService) );
    }

}
