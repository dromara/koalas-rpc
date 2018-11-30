package transport;

import ex.OutMaxLengthException;
import ex.RSAException;
import org.apache.thrift.TByteArrayOutputStream;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import utils.KoalasRsaUtil;

import java.util.Arrays;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class TKoalasFramedTransport extends TTransport {

    protected static final int DEFAULT_MAX_LENGTH = 16384000;

    private int maxLength_;

    private TTransport transport_ = null;

    private final TByteArrayOutputStream writeBuffer_ = new TByteArrayOutputStream ( 1024 );

    private TMemoryInputTransport readBuffer_ = new TMemoryInputTransport ( new byte[0] );

    public static final byte first = (byte) 0xAB;
    public static final byte second = (byte) 0xBA;
    private byte version = (byte) 1;
    private byte heartbeat = (byte) 1;
    private byte rsa = (byte) 0;
    private String privateKey;

    public byte getRsa() {
        return rsa;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public byte getZip() {
        return zip;
    }

    private String publicKey;
    private byte zip = (byte) 0;
    private boolean ifUserProtocol = true;

    public boolean isIfUserProtocol() {
        return ifUserProtocol;
    }

    public void setIfUserProtocol(boolean ifUserProtocol) {
        this.ifUserProtocol = ifUserProtocol;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public void setHeartbeat(byte heartbeat) {
        this.heartbeat = heartbeat;
    }

    public void setRsa(byte rsa) {
        this.rsa = rsa;
    }

    public void setZip(byte zip) {
        this.zip = zip;
    }

    public static class Factory extends TTransportFactory {
        private int maxLength_;
        private boolean user;
        private byte rsa;
        private String privateKey;
        private String publicKey;

        public Factory() {
            maxLength_ = TKoalasFramedTransport.DEFAULT_MAX_LENGTH;
        }
        public boolean isUser() {
            return user;
        }
        public void setUser(boolean user) {
            this.user = user;
        }
        public byte getRsa() {
            return rsa;
        }
        public void setRsa(byte rsa) {
            this.rsa = rsa;
        }
        public String getPrivateKey() {
            return privateKey;
        }
        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
        public String getPublicKey() {
            return publicKey;
        }
        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
        @Override
        public TTransport getTransport(TTransport base) {
            TKoalasFramedTransport tKoalasFramedTransport= new TKoalasFramedTransport ( base, maxLength_ );
            tKoalasFramedTransport.setIfUserProtocol ( user );
            tKoalasFramedTransport.setRsa ( rsa );
            tKoalasFramedTransport.setPrivateKey ( privateKey );
            tKoalasFramedTransport.setPublicKey ( publicKey );
            return tKoalasFramedTransport;
        }
    }

    public TKoalasFramedTransport(byte version, byte heartbeat, byte rsa, byte zip) {
        this.version = version;
        this.heartbeat = heartbeat;
        this.rsa = rsa;
        this.zip = zip;
    }

    public TKoalasFramedTransport(TTransport transport, int maxLength, boolean ifUserProtocol) {
        this ( (byte) 1, (byte) 1, (byte) 0, (byte) 0 );
        transport_ = transport;
        maxLength_ = maxLength;
        this.ifUserProtocol = ifUserProtocol;
    }

    public TKoalasFramedTransport(TTransport transport, int maxLength) {
        this ( (byte) 1, (byte) 1, (byte) 0, (byte) 0 );
        transport_ = transport;
        maxLength_ = maxLength;
    }

    public TKoalasFramedTransport(TTransport transport) {
        this ( (byte) 1, (byte) 1, (byte) 0, (byte) 0 );
        transport_ = transport;
        maxLength_ = TKoalasFramedTransport.DEFAULT_MAX_LENGTH;
    }

    public void open() throws TTransportException {
        transport_.open ();
    }

    public boolean isOpen() {
        return transport_.isOpen ();
    }

    public void close() {
        transport_.close ();
    }

    public int read(byte[] buf, int off, int len) throws TTransportException {
        if (readBuffer_ != null) {
            int got = readBuffer_.read ( buf, off, len );
            if (got > 0) {
                return got;
            }
        }

        // Read another frame of data
        readFrame ();

        return readBuffer_.read ( buf, off, len );
    }

    @Override
    public byte[] getBuffer() {
        return readBuffer_.getBuffer ();
    }

    @Override
    public int getBufferPosition() {
        return readBuffer_.getBufferPosition ();
    }

    @Override
    public int getBytesRemainingInBuffer() {
        return readBuffer_.getBytesRemainingInBuffer ();
    }

    @Override
    public void consumeBuffer(int len) {
        readBuffer_.consumeBuffer ( len );
    }

    private final byte[] i32buf = new byte[4];

    private void readFrame() throws TTransportException {
        transport_.readAll ( i32buf, 0, 4 );
        int size = decodeFrameSize ( i32buf );

        if (size < 0) {
            throw new TTransportException ( "Read a negative frame size (" + size + ")!" );
        }

        if (size > maxLength_) {
            throw new TTransportException ( "Frame size (" + size + ") larger than max length (" + maxLength_ + ")!" );
        }

        byte[] buff = new byte[size];
        transport_.readAll ( buff, 0, size );

        if (buff[0] == first && buff[1] == second) {
            ifUserProtocol = true;
        } else {
            ifUserProtocol = false;
        }

        if (ifUserProtocol) {
            byte[] request = new byte[size - 6];
            byte[] header = new byte[6];
            System.arraycopy ( buff, 6, request, 0, size - 6 );
            System.arraycopy ( buff, 0, header, 0, 6 );

            //RSA
            if (header[4] == (byte) 1) {

                byte[] signLenByte = new byte[4];
                System.arraycopy ( buff, 6, signLenByte, 0, 4 );

                int signLen = decodeFrameSize ( signLenByte );
                byte[] signByte = new byte[signLen];
                System.arraycopy ( buff, 10, signByte, 0, signLen );

                String sign = "";
                try {
                    sign = new String ( signByte, "UTF-8" );
                } catch (Exception e) {
                    throw new RSAException();
                }

                byte[] rsaBody = new byte[size -10-signLen];
                System.arraycopy ( buff, 10+signLen, rsaBody, 0, size -10-signLen );


                try {
                    if(!KoalasRsaUtil.verify ( rsaBody,publicKey,sign )){
                        throw new RSAException("verify error");
                    }
                    request = KoalasRsaUtil.decryptByPrivateKey (rsaBody,privateKey);
                } catch (Exception e) {
                    throw new RSAException("rsa service error",e);
                }
            }

            readBuffer_.reset ( request );
            return;
        }

        readBuffer_.reset ( buff );
    }

    public void write(byte[] buf, int off, int len) throws TTransportException {
        writeBuffer_.write ( buf, off, len );
    }

    public static void main(String[] args) {
        byte[] b = new byte[]{1, 2, 3, 4, 5, 6};
        System.out.println ( Arrays.toString ( Arrays.copyOfRange ( b, 0, 5 ) ) );
    }

    /**
     * |All length(4 Byte)||first(1 Byte),second(1 Byte),version(1 Byte),heartbeat(1 Byte),rsa(1 Byte),zip(1 Byte)|Sign length(4 Byte)|sing body|thrift body|
     **/

    @Override
    public void flush() throws TTransportException {
        //Body buf
        byte[] buf = writeBuffer_.get ();

        //Body + header length
        int len = writeBuffer_.len ();

        if(len > maxLength_){
             throw new OutMaxLengthException ("the length :" + len + "> maxLength_ :" + maxLength_);
        }

        //Body buf length
        int thrift = len;
        writeBuffer_.reset ();
        String sign = "";
        int signlength = 0;
        if (ifUserProtocol) {
            if (rsa == (byte) 1) {
                try {
                    buf = Arrays.copyOfRange ( buf, 0, len );
                    buf = KoalasRsaUtil.encryptByPublicKey ( buf, publicKey );
                    len = buf.length;
                    thrift = len;
                    sign = KoalasRsaUtil.sign ( buf, privateKey );
                    signlength = sign.getBytes ( "UTF-8" ).length;
                    //signlength-int
                    len += 4;
                    //signlength
                    len += signlength;
                } catch (Exception e) {
                    throw new RSAException ( "rsa error by client", e );
                }
            }
            len = len + 6;
        }
        encodeFrameSize ( len, i32buf );
        transport_.write ( i32buf, 0, 4 );

        //the 2.0 version will be add Ras and zip
        if (ifUserProtocol) {
            transport_.write ( new byte[]{first} );
            transport_.write ( new byte[]{second} );
            transport_.write ( new byte[]{version} );
            transport_.write ( new byte[]{heartbeat} );
            transport_.write ( new byte[]{rsa} );
            transport_.write ( new byte[]{zip} );
            if (rsa == (byte) 1) {
                byte[] i32sing = new byte[4];
                encodeFrameSize ( signlength, i32sing );
                transport_.write ( i32sing );
                try {
                    transport_.write ( sign.getBytes ( "UTF-8" ) );
                } catch (Exception e) {
                    // can not got in
                }
            }
        }

        transport_.write ( buf, 0, thrift );
        transport_.flush ();
    }

    public static final void encodeFrameSize(final int frameSize, final byte[] buf) {
        buf[0] = (byte) (0xff & (frameSize >> 24));
        buf[1] = (byte) (0xff & (frameSize >> 16));
        buf[2] = (byte) (0xff & (frameSize >> 8));
        buf[3] = (byte) (0xff & (frameSize));
    }

    public static final int decodeFrameSize(final byte[] buf) {
        return
                ((buf[0] & 0xff) << 24) |
                        ((buf[1] & 0xff) << 16) |
                        ((buf[2] & 0xff) << 8) |
                        ((buf[3] & 0xff));
    }
}
