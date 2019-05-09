/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package thrift;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import exceptions.RSAException;
import heartbeat.impl.HeartbeatServiceImpl;
import heartbeat.service.HeartbeatService;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TByteArrayOutputStream;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.*;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.KoalasBinaryProtocol;
import protocol.KoalasTrace;
import server.domain.ErrorType;
import transport.TKoalasFramedTransport;
import utils.KoalasRsaUtil;
import utils.TraceThreadContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public abstract class KoalasAbstractNonblockingServer extends TServer {
    protected final Logger LOGGER = LoggerFactory.getLogger ( getClass ().getName () );

    public static abstract class AbstractNonblockingServerArgs<T extends AbstractNonblockingServerArgs<T>> extends AbstractServerArgs<T> {
        public long maxReadBufferBytes = Long.MAX_VALUE;

        public AbstractNonblockingServerArgs(TNonblockingServerTransport transport) {
            super ( transport );
            transportFactory ( new TFramedTransport.Factory () );
        }
    }

    private final long MAX_READ_BUFFER_BYTES;

    private final AtomicLong readBufferBytesAllocated = new AtomicLong ( 0 );

    public KoalasAbstractNonblockingServer(AbstractNonblockingServerArgs args) {
        super ( args );
        MAX_READ_BUFFER_BYTES = args.maxReadBufferBytes;
    }

    public void serve() {
        if (!startThreads ()) {
            return;
        }

        if (!startListening ()) {
            return;
        }

        setServing ( true );

        waitForShutdown ();

        setServing ( false );

        stopListening ();
    }

    protected abstract boolean startThreads();

    protected abstract void waitForShutdown();

    protected boolean startListening() {
        try {
            serverTransport_.listen ();
            return true;
        } catch (TTransportException ttx) {
            LOGGER.error ( "Failed to start listening on server socket!", ttx );
            return false;
        }
    }

    protected void stopListening() {
        serverTransport_.close ();
    }

    protected abstract boolean requestInvoke(FrameBuffer frameBuffer);

    protected abstract class AbstractSelectThread extends Thread {
        protected final Selector selector;

        protected final Set<FrameBuffer> selectInterestChanges = new HashSet<FrameBuffer> ();

        public AbstractSelectThread() throws IOException {
            this.selector = SelectorProvider.provider ().openSelector ();
        }

        public void wakeupSelector() {
            selector.wakeup ();
        }

        public void requestSelectInterestChange(FrameBuffer frameBuffer) {
            synchronized (selectInterestChanges) {
                selectInterestChanges.add ( frameBuffer );
            }
            selector.wakeup ();
        }

        protected void processInterestChanges() {
            synchronized (selectInterestChanges) {
                for (FrameBuffer fb : selectInterestChanges) {
                    fb.changeSelectInterests ();
                }
                selectInterestChanges.clear ();
            }
        }

        protected void handleRead(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment ();
            if (!buffer.read ()) {
                cleanupSelectionKey ( key );
                return;
            }

            if (buffer.isFrameFullyRead ()) {
                if (!requestInvoke ( buffer )) {
                    cleanupSelectionKey ( key );
                }
            }
        }

        protected void handleWrite(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment ();
            if (!buffer.write ()) {
                cleanupSelectionKey ( key );
            }
        }

        protected void cleanupSelectionKey(SelectionKey key) {
            FrameBuffer buffer = (FrameBuffer) key.attachment ();
            if (buffer != null) {
                buffer.close ();
            }
            key.cancel ();
        }
    }

    private enum FrameBufferState {
        READING_FRAME_SIZE,
        READING_FRAME,
        READ_FRAME_COMPLETE,
        AWAITING_REGISTER_WRITE,
        WRITING,
        AWAITING_REGISTER_READ,
        AWAITING_CLOSE
    }

    protected class FrameBuffer {
        private final TNonblockingTransport trans_;

        private final SelectionKey selectionKey_;

        private final AbstractSelectThread selectThread_;

        private FrameBufferState state_ = FrameBufferState.READING_FRAME_SIZE;

        private ByteBuffer buffer_;

        private TByteArrayOutputStream response_;

        private String privateKey;
        private String publicKey;
        private String serviceName;
        private String methodName;
        private KoalasTrace koalasTrace;
        private boolean generic;
        private TProcessor tGenericProcessor;
        private boolean cat;
        public FrameBuffer(final TNonblockingTransport trans,
                           final SelectionKey selectionKey,
                           final AbstractSelectThread selectThread,
                           String privateKey,
                           String publicKey,
                           String serviceName,
                           TProcessor tGenericProcessor,
                           boolean cat) {
            this(trans,selectionKey,selectThread);
            this.privateKey= privateKey;
            this.publicKey = publicKey;
            this.serviceName =serviceName;
            this.tGenericProcessor=tGenericProcessor;
            this.cat =cat;
        }

        public FrameBuffer(final TNonblockingTransport trans,
                           final SelectionKey selectionKey,
                           final AbstractSelectThread selectThread) {
            trans_ = trans;
            selectionKey_ = selectionKey;
            selectThread_ = selectThread;
            buffer_ = ByteBuffer.allocate ( 4 );
        }

        public boolean read() {
            if (state_ == FrameBufferState.READING_FRAME_SIZE) {
                if (!internalRead ()) {
                    return false;
                }

                if (buffer_.remaining () == 0) {
                    int frameSize = buffer_.getInt ( 0 );
                    if (frameSize <= 0) {
                        LOGGER.error ( "Read an invalid frame size of " + frameSize
                                + ". Are you using TFramedTransport on the client side?" );
                        return false;
                    }

                    if (frameSize > MAX_READ_BUFFER_BYTES) {
                        LOGGER.error ( "Read a frame size of " + frameSize
                                + ", which is bigger than the maximum allowable buffer size for ALL connections." );
                        return false;
                    }

                    if (readBufferBytesAllocated.get () + frameSize > MAX_READ_BUFFER_BYTES) {
                        return true;
                    }

                    readBufferBytesAllocated.addAndGet ( frameSize );

                    buffer_ = ByteBuffer.allocate ( frameSize );

                    state_ = FrameBufferState.READING_FRAME;
                } else {

                    return true;
                }
            }

            if (state_ == FrameBufferState.READING_FRAME) {
                if (!internalRead ()) {
                    return false;
                }

                if (buffer_.remaining () == 0) {
                    selectionKey_.interestOps ( 0 );
                    state_ = FrameBufferState.READ_FRAME_COMPLETE;
                }

                return true;
            }

            LOGGER.error ( "Read was called but state is invalid (" + state_ + ")" );
            return false;
        }

        public boolean write() {
            if (state_ == FrameBufferState.WRITING) {
                try {
                    if (trans_.write ( buffer_ ) < 0) {
                        return false;
                    }
                } catch (IOException e) {
                    LOGGER.warn ( "Got an IOException during write!", e );
                    return false;
                }

                if (buffer_.remaining () == 0) {
                    prepareRead ();
                }
                return true;
            }

            LOGGER.error ( "Write was called, but state is invalid (" + state_ + ")" );
            return false;
        }

        public void changeSelectInterests() {
            if (state_ == FrameBufferState.AWAITING_REGISTER_WRITE) {
                // set the OP_WRITE interest
                selectionKey_.interestOps ( SelectionKey.OP_WRITE );
                state_ = FrameBufferState.WRITING;
            } else if (state_ == FrameBufferState.AWAITING_REGISTER_READ) {
                prepareRead ();
            } else if (state_ == FrameBufferState.AWAITING_CLOSE) {
                close ();
                selectionKey_.cancel ();
            } else {
                LOGGER.error ( "changeSelectInterest was called, but state is invalid (" + state_ + ")" );
            }
        }

        public void close() {

            if (state_ == FrameBufferState.READING_FRAME || state_ == FrameBufferState.READ_FRAME_COMPLETE) {
                readBufferBytesAllocated.addAndGet ( -buffer_.array ().length );
            }
            trans_.close ();
        }

        public boolean isFrameFullyRead() {
            return state_ == FrameBufferState.READ_FRAME_COMPLETE;
        }

        public void responseReady() {

            readBufferBytesAllocated.addAndGet ( -buffer_.array ().length );

            if (response_.len () == 0) {
                state_ = FrameBufferState.AWAITING_REGISTER_READ;
                buffer_ = null;
            } else {
                buffer_ = ByteBuffer.wrap ( response_.get (), 0, response_.len () );

                state_ = FrameBufferState.AWAITING_REGISTER_WRITE;
            }
            requestSelectInterestChange ();
        }

        public void invoke() {
            try {
                TTransport inTrans = getInputTransport ();
                TTransport outTrans = getOutputTransport ();
                TProtocol inProt = inputProtocolFactory_.getProtocol ( inTrans );
                TProtocol outProt = outputProtocolFactory_.getProtocol ( outTrans );

                byte[] body = buffer_.array ();
                if(body[0]==TKoalasFramedTransport.first && body[1]==TKoalasFramedTransport.second && body[3]==((byte) 2)){
                    TProcessor tprocessorheartbeat = new HeartbeatService.Processor<> (new HeartbeatServiceImpl () );
                    ((TKoalasFramedTransport)outTrans).setHeartbeat ( (byte) 2 );
                    tprocessorheartbeat.process ( inProt,outProt );
                } else{
                    Transaction transaction=null;
                    try {
                        if(StringUtils.isNotEmpty ( methodName ) && cat){
                            transaction = Cat.newTransaction("Service", serviceName.concat ( "." ).concat ( methodName ));
                            if(koalasTrace.getRootId ()!= null){
                                String rootId = koalasTrace.getRootId ();
                                String childId = koalasTrace.getChildId ();
                                String parentId = koalasTrace.getParentId ();
                                MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
                                tree.setParentMessageId(parentId);
                                tree.setRootMessageId(rootId);
                                tree.setMessageId(childId);
                                //String child_Id = Cat.getProducer().createRpcServerId("default");
                                //Cat.logEvent(CatConstants.TYPE_REMOTE_CALL, "", Event.SUCCESS, child_Id);
                                KoalasTrace currentKoalasTrace = new KoalasTrace();
                                currentKoalasTrace.setParentId ( childId );
                                currentKoalasTrace.setRootId ( rootId );
                                //CurrentKoalasTrace.setChildId ( child_Id );
                                TraceThreadContext.set (currentKoalasTrace);
                            }
                        }

                        try {
                            if(!generic){
                                processorFactory_.getProcessor ( inTrans ).process ( inProt, outProt );
                            } else {
                                tGenericProcessor.process (  inProt, outProt  );
                            }
                        } catch (Exception e){
                            byte[] _body = buffer_.array ();
                            byte[] len = new byte[4];
                            encodeFrameSize ( _body.length, len );
                            byte[] _b = new byte[_body.length + 4];
                            System.arraycopy ( len, 0, _b, 0, 4 );
                            System.arraycopy ( _body, 0, _b, 4, _body.length );
                            handlerException(_b,e,ErrorType.APPLICATION,privateKey,publicKey);
                        }

                        if(transaction!=null && cat)
                            transaction.setStatus ( Transaction.SUCCESS );
                    } catch (Exception e){
                        if(transaction!=null && cat)
                            transaction.setStatus ( e );
                        throw  e;
                    } finally {
                        if(transaction!=null && cat)
                            transaction.complete ();
                        if(koalasTrace.getRootId ()!= null && cat){
                            TraceThreadContext.remove ();
                        }
                    }
                }

                responseReady ();
                return;
            } catch (TException te) {
                LOGGER.warn ( "Exception while invoking!", te );
            } catch (Throwable t) {
                LOGGER.error ( "Unexpected throwable while invoking!", t );
            }
            state_ = FrameBufferState.AWAITING_CLOSE;
            requestSelectInterestChange ();
        }

        public void handlerException(byte[] b, Exception e, ErrorType type, String privateKey, String publicKey) throws TException {

            String value = MessageFormat.format("thrift server error，the error message is: {0}",e.getMessage ());
            boolean ifUserProtocol;
            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
                ifUserProtocol = true;
            }else{
                ifUserProtocol = false;
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
            response_ = new TByteArrayOutputStream (  );

            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
            TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport (  response_);

            TKoalasFramedTransport inTransport = new TKoalasFramedTransport( tioStreamTransportInput );
            TKoalasFramedTransport outTransport = new TKoalasFramedTransport ( tioStreamTransportOutput,16384000,ifUserProtocol );

            TProtocolFactory tProtocolFactory =new KoalasBinaryProtocol.Factory();

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
                            return;
                        } catch (Exception e2){
                            LOGGER.error ( e2.getMessage (),e2);
                            throw  e2;
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
                            return;
                        } catch (Exception e2){
                            LOGGER.error ( e2.getMessage (),e2);
                            throw  e2;
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
                LOGGER.info ( "handlerException:" + tApplicationException.getType () + value );
            } catch (Exception e1) {
                LOGGER.error ( "unknown Exception:" + type + value,e1 );
                throw  e1;
            }

        }

        private TTransport getInputTransport() {

            byte[] body = buffer_.array ();

            byte[] len = new byte[4];
            encodeFrameSize ( body.length, len );
            byte[] b = new byte[body.length + 4];

            System.arraycopy ( len, 0, b, 0, 4 );
            System.arraycopy ( body, 0, b, 4, body.length );
            TMessage tMessage;
            KoalasTrace koalasTrace;
            String genericMethodName;
            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
                KoalasMessage koalasMessage =  getKoalasTMessage ( b);
                tMessage = koalasMessage.gettMessage ();
                koalasTrace = koalasMessage.getKoalasTrace ();
                generic = koalasMessage.isGeneric ();
                genericMethodName=koalasMessage.getGenericMethodName ();
            } else{
                KoalasMessage koalasMessage = getTMessage ( b);
                tMessage =koalasMessage.gettMessage ();
                koalasTrace =koalasMessage.getKoalasTrace ();
                generic = koalasMessage.isGeneric ();
                genericMethodName=koalasMessage.getGenericMethodName ();
            }

            methodName = generic? genericMethodName:tMessage.name;
            this.koalasTrace = koalasTrace;
            if(this.privateKey != null && this.publicKey!=null){
                if(b[8] != (byte) 1 || !(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second)){
                    throw new RSAException ( "thrift server rsa error" );
                }
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream ( b );
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport ( inputStream );
            TKoalasFramedTransport inTransport = new TKoalasFramedTransport ( tioStreamTransportInput );

            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
                if(b[8] == (byte) 1){
                    //in
                    inTransport.setPrivateKey ( this.privateKey );
                    inTransport.setPublicKey ( this.publicKey );
                }
            }

            return inTransport;
        }

        private KoalasMessage getTMessage(byte[] b){

            byte[] buff = new byte[b.length-4];
            System.arraycopy (  b,4,buff,0,buff.length);
            ByteArrayInputStream inputStream = new ByteArrayInputStream ( buff );
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
            TProtocol tBinaryProtocol = new KoalasBinaryProtocol ( tioStreamTransportInput,true );
            TMessage tMessage;
            KoalasTrace koalasTrace;
            String genericMethodName;

            boolean generic;
            try {
                tMessage= tBinaryProtocol.readMessageBegin ();
                koalasTrace = ((KoalasBinaryProtocol) tBinaryProtocol).getKoalasTrace ();
                generic = ((KoalasBinaryProtocol) tBinaryProtocol).isGeneric ();
                genericMethodName = ((KoalasBinaryProtocol) tBinaryProtocol).getGenericMethodName ();

            } catch (TException e) {
                return new KoalasMessage (new TMessage(),new KoalasTrace(),false,StringUtils.EMPTY);

            }
            return new KoalasMessage(tMessage,koalasTrace,generic,genericMethodName);
        }

        private KoalasMessage getKoalasTMessage(byte[] b){

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
                    return new KoalasMessage(new TMessage(),new KoalasTrace(),false,StringUtils.EMPTY);
                }

                byte[] rsaBody = new byte[size -10-signLen];
                System.arraycopy ( buff, 10+signLen, rsaBody, 0, size -10-signLen );

                try {
                    if(!KoalasRsaUtil.verify ( rsaBody,publicKey,sign )){
                        return new KoalasMessage(new TMessage(),new KoalasTrace(),false,StringUtils.EMPTY);
                    }
                    request = KoalasRsaUtil.decryptByPrivateKey (rsaBody,privateKey);
                } catch (Exception e) {
                    return new KoalasMessage(new TMessage(),new KoalasTrace(),false,StringUtils.EMPTY);
                }
            }
            TMessage tMessage;
            KoalasTrace koalasTrace;
            boolean generic;
            String genericMethodName;
            ByteArrayInputStream inputStream = new ByteArrayInputStream ( request );
            TIOStreamTransport tioStreamTransportInput = new TIOStreamTransport (  inputStream);
            try {
                TProtocol tBinaryProtocol = new KoalasBinaryProtocol( tioStreamTransportInput,true );
                tMessage= tBinaryProtocol.readMessageBegin ();
                koalasTrace = ((KoalasBinaryProtocol) tBinaryProtocol).getKoalasTrace ();
                generic = ((KoalasBinaryProtocol) tBinaryProtocol).isGeneric ();
                genericMethodName = ((KoalasBinaryProtocol) tBinaryProtocol).getGenericMethodName ();
            } catch (TException e) {
                return new KoalasMessage(new TMessage(),new KoalasTrace(),false,StringUtils.EMPTY);
            }
            return new KoalasMessage(tMessage,koalasTrace,generic,genericMethodName);
        }

        private  class KoalasMessage{
            private TMessage tMessage;
            private KoalasTrace koalasTrace;
            private boolean generic;
            private String genericMethodName;

            public KoalasMessage(TMessage tMessage, KoalasTrace koalasTrace, boolean generic,String genericMethodName) {
                this.tMessage = tMessage;
                this.koalasTrace = koalasTrace;
                this.generic = generic;
                this.genericMethodName=genericMethodName;

            }

            public String getGenericMethodName() {
                return genericMethodName;
            }

            public void setGenericMethodName(String genericMethodName) {
                this.genericMethodName = genericMethodName;
            }

            public TMessage gettMessage() {
                return tMessage;
            }

            public void settMessage(TMessage tMessage) {
                this.tMessage = tMessage;
            }

            public KoalasTrace getKoalasTrace() {
                return koalasTrace;
            }

            public void setKoalasTrace(KoalasTrace koalasTrace) {
                this.koalasTrace = koalasTrace;
            }

            public boolean isGeneric() {
                return generic;
            }

            public void setGeneric(boolean generic) {
                this.generic = generic;
            }
        }

        public void encodeFrameSize(int frameSize, byte[] buf) {
            buf[0] = (byte) (0xff & (frameSize >> 24));
            buf[1] = (byte) (0xff & (frameSize >> 16));
            buf[2] = (byte) (0xff & (frameSize >> 8));
            buf[3] = (byte) (0xff & (frameSize));
        }

        private TTransport getOutputTransport() {
            byte[] body = buffer_.array ();

            byte[] len = new byte[4];
            encodeFrameSize ( body.length, len );
            byte[] b = new byte[body.length + 4];

            System.arraycopy ( len, 0, b, 0, 4 );
            System.arraycopy ( body, 0, b, 4, body.length );


            boolean ifUserProtocol;
            if (b[4] == TKoalasFramedTransport.first && b[5] == TKoalasFramedTransport.second) {
                ifUserProtocol = true;
            } else {
                ifUserProtocol = false;
            }
            response_= new TByteArrayOutputStream ();
            TIOStreamTransport tioStreamTransportOutput = new TIOStreamTransport ( response_ );
            TKoalasFramedTransport outTransport = new TKoalasFramedTransport ( tioStreamTransportOutput, 2048000, ifUserProtocol );

            if(b[4]==TKoalasFramedTransport.first && b[5]==TKoalasFramedTransport.second){
                if(b[8] == (byte) 1){
                    //out
                    outTransport.setRsa ( (byte) 1 );
                    outTransport.setPrivateKey ( this.privateKey );
                    outTransport.setPublicKey ( this.publicKey );
                }
            }
            return outTransport;
        }


        private boolean internalRead() {
            try {
                if (trans_.read ( buffer_ ) < 0) {
                    return false;
                }
                return true;
            } catch (IOException e) {
                LOGGER.warn ( "Got an IOException in internalRead!", e );
                return false;
            }
        }

        private void prepareRead() {

            selectionKey_.interestOps ( SelectionKey.OP_READ );
            buffer_ = ByteBuffer.allocate ( 4 );
            state_ = FrameBufferState.READING_FRAME_SIZE;
        }

        private void requestSelectInterestChange() {
            if (Thread.currentThread () == this.selectThread_) {
                changeSelectInterests ();
            } else {
                this.selectThread_.requestSelectInterestChange ( this );
            }
        }
    }
}
