package netty.hanlder;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import ex.RSAException;
import heartbeat.impl.HeartbeatServiceImpl;
import heartbeat.service.HeartbeatService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import monitor.KoalasCatCtx;
import monitor.KoalasContext;
import monitor.KoalasLocalThreadCtx;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TIOStreamTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.KoalasTBinaryProtocol;
import server.domain.ErrorType;
import transport.TKoalasFramedTransport;
import utils.KoalasRsaUtil;

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

    private String className;

    public KoalasHandler(TProcessor tprocessor, ExecutorService executorService, String privateKey, String publicKey, String className) {
        this.tprocessor = tprocessor;
        this.executorService = executorService;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.className = className;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        try {
            int i =msg.readableBytes ();
            byte[] b = new byte[i];
            msg.readBytes ( b );

            KoalasContext koalasContext=null;
            TMessage tMessage=null;
            boolean ifUserProtocol;
            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
                ifUserProtocol = true;
                koalasInner koalasInner = getKoalasInner ( b);
                koalasContext = koalasInner.getKoalasContext ();
                tMessage = koalasInner.gettMessage ();
                KoalasCatCtx koalasCatCtx = new KoalasCatCtx ();
                koalasCatCtx.setKoalasContext ( koalasContext );

                KoalasLocalThreadCtx.set (koalasCatCtx);
            }else{
                ifUserProtocol = false;
                tMessage =getTMessage ( b );
                koalasContext = new KoalasContext (  );
                KoalasCatCtx koalasCatCtx = new KoalasCatCtx ();
                koalasCatCtx.setKoalasContext ( koalasContext );
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream (  );

            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
            TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport (  outputStream);

            TKoalasFramedTransport inTransport = new TKoalasFramedTransport( tioStreamTransportInput,2048000 );
            TKoalasFramedTransport outTransport = new TKoalasFramedTransport ( tioStreamTransportOutput,2048000,ifUserProtocol );

            if(this.privateKey != null && this.publicKey!=null){
                if(b[8] != (byte) 1 || !(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second)){
                    logger.error ("rsa error the client is not ras support!");
                    handlerException(b,ctx,new RSAException ( "rsa error" ),ErrorType.APPLICATION,privateKey,publicKey);
                    return;
                }
            }

            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){

                //heartbeat
                if(b[7] == (byte) 2){
                    TProcessor tprocessorheartbeat = new HeartbeatService.Processor<> (new HeartbeatServiceImpl () );
                    outTransport.setHeartbeat ( (byte) 2 );
                    TProtocolFactory tProtocolFactory =new TBinaryProtocol.Factory();
                    TProtocol in =tProtocolFactory.getProtocol ( inTransport );
                    TProtocol out =tProtocolFactory.getProtocol ( outTransport );
                    try {
                        tprocessorheartbeat.process ( in,out );
                        ctx.writeAndFlush ( outputStream );
                        return;
                    } catch (Exception e){
                        logger.error ( "heartbeat error e" );
                        handlerException(b,ctx,e,ErrorType.APPLICATION,privateKey,publicKey);
                        return;
                    }
                }

                //rsa support
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
            TProtocolFactory tProtocolFactory;
            if(ifUserProtocol){
                tProtocolFactory =new KoalasTBinaryProtocol.Factory (  );
            }else {
                tProtocolFactory =new TBinaryProtocol.Factory();
            }
            TProtocol in =tProtocolFactory.getProtocol ( inTransport );
            TProtocol out =tProtocolFactory.getProtocol ( outTransport );
            try {
                executorService.execute ( new NettyRunable (  ctx,in,out,outputStream,tprocessor,b,privateKey,publicKey,koalasContext,className,tMessage.name));
            } catch (RejectedExecutionException e){
                logger.error ( e.getMessage ()+ErrorType.THREAD,e );
                handlerException(b,ctx,e,ErrorType.THREAD,privateKey,publicKey);
            }
        } finally {
            KoalasLocalThreadCtx.remove ();
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
        private KoalasContext koalasContext;
        private String className;
        private String methodName;

        public NettyRunable(ChannelHandlerContext ctx, TProtocol in, TProtocol out, ByteArrayOutputStream outputStream, TProcessor tprocessor, byte[] b, String privateKey, String publicKey, KoalasContext koalasContext, String className, String methodname) {
            this.ctx = ctx;
            this.in = in;
            this.out = out;
            this.outputStream = outputStream;
            this.tprocessor = tprocessor;
            this.b = b;
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.koalasContext = koalasContext;
            this.className = className;
            this.methodName = methodname;
        }

        @Override
        public void run() {
            Transaction transaction = Cat.newTransaction("Service", className.concat ( "." ).concat ( methodName ));
            KoalasCatCtx koalasCatCtx = new KoalasCatCtx();
            koalasCatCtx.setKoalasContext ( koalasContext );
            if(koalasContext.getCatParentMessageId ()!= null){
                Cat.logRemoteCallServer ( koalasCatCtx );
            }
            Cat.logRemoteCallClient ( koalasCatCtx );
            KoalasLocalThreadCtx.set ( koalasCatCtx );
            try {
                tprocessor.process ( in,out );
                ctx.writeAndFlush (outputStream);
                transaction.setStatus ( Transaction.SUCCESS );
            } catch (Exception e) {
                transaction.setStatus ( e );
                logger.error ( e.getMessage () + ErrorType.APPLICATION,e );
                handlerException(this.b,ctx,e,ErrorType.APPLICATION,privateKey,publicKey);
            }finally {
                transaction.complete ();
                KoalasLocalThreadCtx.remove ();
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

        TProtocolFactory tProtocolFactory;
        if(ifUserProtocol){
              tProtocolFactory =new KoalasTBinaryProtocol.Factory (  );

        }else{
              tProtocolFactory =new TBinaryProtocol.Factory();
        }

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

    private static String getClientIP(ChannelHandlerContext ctx) {
        String ip;
        try {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            ip = socketAddress.getAddress().getHostAddress();
        } catch (Exception e) {
            ip = "unknown";
        }
        return ip;
    }

    private TMessage getTMessage(byte[] b){

        byte[] buff = new byte[b.length-4];
        System.arraycopy (  b,4,buff,0,buff.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream ( buff );
        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        TProtocol tBinaryProtocol = new KoalasTBinaryProtocol( tioStreamTransportInput );
        TMessage tMessage=null;
        try {
             tMessage= tBinaryProtocol.readMessageBegin ();
             return tMessage;
        } catch (TException e) {
        }

        return new TMessage();
    }

    private koalasInner getKoalasInner(byte[] b){
        koalasInner koalasInner = new koalasInner (  );
        byte[] buff = new byte[b.length-4];
        System.arraycopy (  b,4,buff,0,buff.length);

        int size = buff.length;
        byte[] request = new byte[size - 6];
        byte[] header = new byte[6];
        System.arraycopy ( buff, 6, request, 0, size - 6 );
        System.arraycopy ( buff, 0, header, 0, 6 );

        //RSA
        if (header[4] == (byte) 1) {

            byte[] signLenByte = new byte[4];
            System.arraycopy ( buff, 6, signLenByte, 0, 4 );

            int signLen = TKoalasFramedTransport.decodeFrameSize ( signLenByte );
            byte[] signByte = new byte[signLen];
            System.arraycopy ( buff, 10, signByte, 0, signLen );

            String sign = "";
            try {
                sign = new String ( signByte, "UTF-8" );
            } catch (Exception e) {
                koalasInner.setKoalasContext ( new KoalasContext() );
                return koalasInner;
            }

            byte[] rsaBody = new byte[size -10-signLen];
            System.arraycopy ( buff, 10+signLen, rsaBody, 0, size -10-signLen );

            try {
                if(!KoalasRsaUtil.verify ( rsaBody,publicKey,sign )){
                    koalasInner.setKoalasContext ( new KoalasContext() );
                    return koalasInner;
                }
                request = KoalasRsaUtil.decryptByPrivateKey (rsaBody,privateKey);
            } catch (Exception e) {
                koalasInner.setKoalasContext ( new KoalasContext() );
                return koalasInner;
            }
        }

        KoalasContext koalasContext = new KoalasContext();
        ByteArrayInputStream inputStream = new ByteArrayInputStream ( request );
        TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
        try {
            TProtocol tBinaryProtocol = new KoalasTBinaryProtocol( tioStreamTransportInput );
            TMessage tMessage= tBinaryProtocol.readMessageBegin ();
            koalasContext = ((KoalasTBinaryProtocol) tBinaryProtocol).koalasContext;
            koalasInner.settMessage ( tMessage );
            koalasInner.setKoalasContext ( koalasContext );
        } catch (TException e) {
            koalasInner.setKoalasContext ( new KoalasContext() );
            return koalasInner;
        }

        return koalasInner;
    }

    private class koalasInner{
        private TMessage tMessage;
        private KoalasContext koalasContext;

        public koalasInner() {
        }

        public TMessage gettMessage() {
            return tMessage;
        }

        public void settMessage(TMessage tMessage) {
            this.tMessage = tMessage;
        }

        public KoalasContext getKoalasContext() {
            return koalasContext;
        }

        public void setKoalasContext(KoalasContext koalasContext) {
            this.koalasContext = koalasContext;
        }

        public koalasInner(TMessage tMessage, KoalasContext koalasContext) {

            this.tMessage = tMessage;
            this.koalasContext = koalasContext;
        }
    }

}
