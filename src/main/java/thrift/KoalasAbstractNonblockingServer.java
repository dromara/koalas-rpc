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

import ex.RSAException;
import org.apache.thrift.TByteArrayOutputStream;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transport.TKoalasFramedTransport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
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

        public FrameBuffer(final TNonblockingTransport trans,
                           final SelectionKey selectionKey,
                           final AbstractSelectThread selectThread,String privateKey,String publicKey) {
            this(trans,selectionKey,selectThread);
            this.privateKey= privateKey;
            this.publicKey = publicKey;
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
                processorFactory_.getProcessor ( inTrans ).process ( inProt, outProt );
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

        private TTransport getInputTransport() {

            byte[] body = buffer_.array ();

            byte[] len = new byte[4];
            encodeFrameSize ( body.length, len );
            byte[] b = new byte[body.length + 4];

            System.arraycopy ( len, 0, b, 0, 4 );
            System.arraycopy ( body, 0, b, 4, body.length );


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
            TKoalasFramedTransport outTransport = new TKoalasFramedTransport ( tioStreamTransportOutput, 16384000, ifUserProtocol );

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
