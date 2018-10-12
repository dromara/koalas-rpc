package netty.hanlder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.domain.ErrorType;
import transport.TKoalasFramedTransport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

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

        boolean ifUserProtocol;
        if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
            ifUserProtocol = true;
        }else{
            ifUserProtocol = false;
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream (  );

        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport (  outputStream);

        TKoalasFramedTransport inTransport = new TKoalasFramedTransport( tioStreamTransportInput );
        TKoalasFramedTransport outTransport = new TKoalasFramedTransport ( tioStreamTransportOutput,16384000,ifUserProtocol );

        TProtocolFactory tProtocolFactory =new TBinaryProtocol.Factory();
        TProtocol in =tProtocolFactory.getProtocol ( inTransport );
        TProtocol out =tProtocolFactory.getProtocol ( outTransport );
        try {
            executorService.execute ( new NettyRunable (  ctx,in,out,outputStream,tprocessor,b));
        } catch (RejectedExecutionException e){
            handlerException(b,ctx,e,ErrorType.THREAD);

        }
    }

    public static class NettyRunable implements  Runnable{

        private ChannelHandlerContext ctx;
        private TProtocol in;
        private TProtocol out;
        private ByteArrayOutputStream outputStream;
        private TProcessor tprocessor;
        private byte[] b;

        public NettyRunable(ChannelHandlerContext ctx, TProtocol in, TProtocol out, ByteArrayOutputStream outputStream, TProcessor tprocessor, byte[] b) {
            this.ctx = ctx;
            this.in = in;
            this.out = out;
            this.outputStream = outputStream;
            this.tprocessor = tprocessor;
            this.b = b;
        }

        @Override
        public void run() {
            try {
                tprocessor.process ( in,out );
                ctx.writeAndFlush (outputStream);
            } catch (Exception e) {
                logger.error ( e.getMessage (),e );
                handlerException(this.b,ctx,e,ErrorType.APPLICATION);
            }
        }

    }

    public static void handlerException(byte[] b, ChannelHandlerContext ctx, Exception e, ErrorType type){
        ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream (  );

        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport (  outputStream);

        TFramedTransport inTransport = new TFramedTransport ( tioStreamTransportInput );
        TFramedTransport outTransport = new TFramedTransport ( tioStreamTransportOutput );


        TProtocolFactory tProtocolFactory =new TBinaryProtocol.Factory();
        TProtocol in =tProtocolFactory.getProtocol ( inTransport );
        TProtocol out =tProtocolFactory.getProtocol ( outTransport );

        String clientIP = getClientIP(ctx);

        String value = MessageFormat.format("the remote ip: {0} invoke error ,the error message is: {1}", clientIP,e.getMessage ());

        try {
            TMessage message  = in.readMessageBegin ();
            TProtocolUtil.skip(in, TType.STRUCT);
            in.readMessageEnd();

            TApplicationException tApplicationException=null;
            switch (type){
                case THREAD:
                    tApplicationException = new TApplicationException(6666,value);
                    break;
                case APPLICATION:
                    tApplicationException = new TApplicationException(TApplicationException.INTERNAL_ERROR,value);
                    break;
            }

            out.writeMessageBegin(new TMessage(message.name, TMessageType.EXCEPTION, message.seqid));
            tApplicationException.write (out  );
            out.writeMessageEnd();
            out.getTransport ().flush ();
            ctx.writeAndFlush ( outputStream);
        } catch (TException e1) {
            logger.error ( "unknown Exception:",e );
        }
    }

    public static String getClientIP(ChannelHandlerContext ctx) {
        String ip;
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            ip = socketAddress.getAddress().getHostAddress();
        } catch (Exception e) {
            ip = "unknown";
        }
        return ip;
    }
}
