package netty.hanlder;

import ex.RSAException;
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
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalasHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final static Logger logger = LoggerFactory.getLogger ( KoalasHandler.class );

    private TProcessor tprocessor;

    private ExecutorService executorService;

    private String privateKey;

    private String publicKey;

    public KoalasHandler(TProcessor tprocessor, ExecutorService executorService, String privateKey, String publicKey) {
        this.tprocessor = tprocessor;
        this.executorService = executorService;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
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

        if(this.privateKey != null && this.publicKey!=null){
            if(b[8] != (byte) 1 || !(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second)){
                logger.error ("rsa error the client is not ras support!");
                handlerException(b,ctx,new RSAException ( "rsa error" ),ErrorType.APPLICATION,privateKey,publicKey);
                return;
            }
        }

        if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
            if(b[8] == (byte) 1){
                //in
                inTransport.setPrivateKey ( this.privateKey );
                inTransport.setPublicKey ( this.publicKey );

                //out
                outTransport.setRsa ( (byte) 1 );
                outTransport.setPrivateKey ( this.privateKey );
                outTransport.setPublicKey ( this.publicKey );
            }
        }

        TProtocolFactory tProtocolFactory =new TBinaryProtocol.Factory();
        TProtocol in =tProtocolFactory.getProtocol ( inTransport );
        TProtocol out =tProtocolFactory.getProtocol ( outTransport );
        try {
            executorService.execute ( new NettyRunable (  ctx,in,out,outputStream,tprocessor,b,privateKey,publicKey));
        } catch (RejectedExecutionException e){
            logger.error ( e.getMessage ()+ErrorType.THREAD,e );
            handlerException(b,ctx,e,ErrorType.THREAD,privateKey,publicKey);
        }
    }

    public static class NettyRunable implements  Runnable{

        private ChannelHandlerContext ctx;
        private TProtocol in;
        private TProtocol out;
        private ByteArrayOutputStream outputStream;
        private TProcessor tprocessor;
        private byte[] b;
        private String privateKey;
        private String publicKey;

        public NettyRunable(ChannelHandlerContext ctx, TProtocol in, TProtocol out, ByteArrayOutputStream outputStream, TProcessor tprocessor, byte[] b, String privateKey, String publicKey) {
            this.ctx = ctx;
            this.in = in;
            this.out = out;
            this.outputStream = outputStream;
            this.tprocessor = tprocessor;
            this.b = b;
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        @Override
        public void run() {
            try {
                tprocessor.process ( in,out );
                ctx.writeAndFlush (outputStream);
            } catch (Exception e) {
                logger.error ( e.getMessage () + ErrorType.APPLICATION,e );
                handlerException(this.b,ctx,e,ErrorType.APPLICATION,privateKey,publicKey);
            }
        }

    }

    public static void handlerException(byte[] b, ChannelHandlerContext ctx, Exception e, ErrorType type, String privateKey, String publicKey){

        String clientIP = getClientIP(ctx);

        String value = MessageFormat.format("the remote ip: {0} invoke error ,the error message is: {1}", clientIP,e.getMessage ());

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

        if(ifUserProtocol){
            //rsa support
            if(b[8]==(byte)1){
                if(!(e instanceof RSAException)){
                    //in
                    inTransport.setPrivateKey ( privateKey );
                    inTransport.setPublicKey ( publicKey );

                    //out
                    outTransport.setRsa ( (byte) 1 );
                    outTransport.setPrivateKey (privateKey );
                    outTransport.setPublicKey ( publicKey );
                } else{
                    try {
                        TMessage tMessage =  new TMessage("", TMessageType.EXCEPTION, -1);
                        TApplicationException exception = new TApplicationException(9999,"【rsa error】:" + value);
                        out.writeMessageBegin ( tMessage );
                        exception.write (out  );
                        out.writeMessageEnd();
                        out.getTransport ().flush ();
                        ctx.writeAndFlush ( outputStream);
                        return;
                    } catch (Exception e2){
                        logger.error ( e2.getMessage (),e2);
                    }
                }
            } else{
                if(e instanceof RSAException){
                    try {
                        TMessage tMessage =  new TMessage("", TMessageType.EXCEPTION, -1);
                        TApplicationException exception = new TApplicationException(6699,"【rsa error】:" + value);
                        out.writeMessageBegin ( tMessage );
                        exception.write (out  );
                        out.writeMessageEnd();
                        out.getTransport ().flush ();
                        ctx.writeAndFlush ( outputStream);
                        return;
                    } catch (Exception e2){
                        logger.error ( e2.getMessage (),e2);
                    }
                }
            }
        }

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
            logger.info ( "handlerException:" + tApplicationException.getType () + value );
        } catch (TException e1) {
            logger.error ( "unknown Exception:" + type + value,e1 );
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
