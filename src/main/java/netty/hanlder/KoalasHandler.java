package netty.hanlder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import netty.NettyServer;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;

public class KoalasHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final static Logger logger = LoggerFactory.getLogger ( KoalasHandler.class );

    private TProcessor tprocessor;

    private ExecutorService executorService;

    public KoalasHandler(TProcessor tprocessor, ExecutorService executorService) {
        this.tprocessor = tprocessor;
        this.executorService = executorService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        int i =msg.readableBytes ();
        byte[] b = new byte[i];
        msg.readBytes ( b );

        ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream (  );

        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport (  outputStream);

        TFramedTransport inTransport = new TFramedTransport ( tioStreamTransportInput );
        TFramedTransport outTransport = new TFramedTransport ( tioStreamTransportOutput );

        TProtocolFactory tProtocolFactory =new TBinaryProtocol.Factory();
        TProtocol in =tProtocolFactory.getProtocol ( inTransport );
        TProtocol out =tProtocolFactory.getProtocol ( outTransport );
        executorService.execute ( new NettyRunable (  ctx,in,out,outputStream,tprocessor));
    }

    public static class NettyRunable implements  Runnable{

        private ChannelHandlerContext ctx;
        private TProtocol in;
        private TProtocol out;
        private ByteArrayOutputStream outputStream;
        private TProcessor tprocessor;

        public NettyRunable(ChannelHandlerContext ctx, TProtocol in, TProtocol out, ByteArrayOutputStream outputStream, TProcessor tprocessor) {
            this.ctx = ctx;
            this.in = in;
            this.out = out;
            this.outputStream = outputStream;
            this.tprocessor = tprocessor;
        }

        @Override
        public void run() {
            try {
                tprocessor.process ( in,out );
            } catch (TException e) {
                logger.error ( e.getMessage (),e );
            }
            ctx.writeAndFlush (outputStream);
        }
    }
}
