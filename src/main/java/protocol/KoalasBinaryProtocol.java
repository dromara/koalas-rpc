//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package protocol;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;
import utils.TraceThreadContext;
/**
 * Copyright (C) 2019
 * All rights reserved
 * User: yulong.zhang
 * Date:2019年04月17日13:46:40
 */
public class KoalasBinaryProtocol extends TProtocol {
    private static final TStruct ANONYMOUS_STRUCT = new TStruct();
    protected static final int VERSION_MASK = -65536;
    protected static final int VERSION_1 = -2147418112;
    protected boolean strictRead_;
    protected boolean strictWrite_;
    protected int readLength_;
    protected boolean checkReadLength_;
    private byte[] bout;
    private byte[] i16out;
    private byte[] i32out;
    private byte[] i64out;
    private byte[] bin;
    private byte[] i16rd;
    private byte[] i32rd;
    private byte[] i64rd;

    public KoalasBinaryProtocol(TTransport trans) {
        this(trans, false, true);
    }

    public KoalasBinaryProtocol(TTransport trans, boolean strictRead, boolean strictWrite) {
        super(trans);
        this.strictRead_ = false;
        this.strictWrite_ = true;
        this.checkReadLength_ = false;
        this.bout = new byte[1];
        this.i16out = new byte[2];
        this.i32out = new byte[4];
        this.i64out = new byte[8];
        this.bin = new byte[1];
        this.i16rd = new byte[2];
        this.i32rd = new byte[4];
        this.i64rd = new byte[8];
        this.strictRead_ = strictRead;
        this.strictWrite_ = strictWrite;
    }

    public void writeMessageBegin(TMessage message) throws TException {
        if (this.strictWrite_) {
            int version = -2147418112 | message.type;
            this.writeI32(version);
            this.writeString(message.name);
            this.writeI32(message.seqid);
        } else {
            this.writeString(message.name);
            this.writeByte(message.type);
            this.writeI32(message.seqid);
        }

        KoalasTrace koalasTrace= TraceThreadContext.get ();
        if(koalasTrace==null){
            koalasTrace = new KoalasTrace();
        }
        koalasTrace.write ( this );
    }

    public void writeMessageEnd() {
    }

    public void writeStructBegin(TStruct struct) {
    }

    public void writeStructEnd() {
    }

    public void writeFieldBegin(TField field) throws TException {
        this.writeByte(field.type);
        this.writeI16(field.id);
    }

    public void writeFieldEnd() {
    }

    public void writeFieldStop() throws TException {
        this.writeByte((byte)0);
    }

    public void writeMapBegin(TMap map) throws TException {
        this.writeByte(map.keyType);
        this.writeByte(map.valueType);
        this.writeI32(map.size);
    }

    public void writeMapEnd() {
    }

    public void writeListBegin(TList list) throws TException {
        this.writeByte(list.elemType);
        this.writeI32(list.size);
    }

    public void writeListEnd() {
    }

    public void writeSetBegin(TSet set) throws TException {
        this.writeByte(set.elemType);
        this.writeI32(set.size);
    }

    public void writeSetEnd() {
    }

    public void writeBool(boolean b) throws TException {
        this.writeByte((byte)(b ? 1 : 0));
    }

    public void writeByte(byte b) throws TException {
        this.bout[0] = b;
        this.trans_.write(this.bout, 0, 1);
    }

    public void writeI16(short i16) throws TException {
        this.i16out[0] = (byte)(255 & i16 >> 8);
        this.i16out[1] = (byte)(255 & i16);
        this.trans_.write(this.i16out, 0, 2);
    }

    public void writeI32(int i32) throws TException {
        this.i32out[0] = (byte)(255 & i32 >> 24);
        this.i32out[1] = (byte)(255 & i32 >> 16);
        this.i32out[2] = (byte)(255 & i32 >> 8);
        this.i32out[3] = (byte)(255 & i32);
        this.trans_.write(this.i32out, 0, 4);
    }

    public void writeI64(long i64) throws TException {
        this.i64out[0] = (byte)((int)(255L & i64 >> 56));
        this.i64out[1] = (byte)((int)(255L & i64 >> 48));
        this.i64out[2] = (byte)((int)(255L & i64 >> 40));
        this.i64out[3] = (byte)((int)(255L & i64 >> 32));
        this.i64out[4] = (byte)((int)(255L & i64 >> 24));
        this.i64out[5] = (byte)((int)(255L & i64 >> 16));
        this.i64out[6] = (byte)((int)(255L & i64 >> 8));
        this.i64out[7] = (byte)((int)(255L & i64));
        this.trans_.write(this.i64out, 0, 8);
    }

    public void writeDouble(double dub) throws TException {
        this.writeI64(Double.doubleToLongBits(dub));
    }

    public void writeString(String str) throws TException {
        try {
            byte[] dat = str.getBytes("UTF-8");
            this.writeI32(dat.length);
            this.trans_.write(dat, 0, dat.length);
        } catch (UnsupportedEncodingException var3) {
            throw new TException("JVM DOES NOT SUPPORT UTF-8");
        }
    }

    public void writeBinary(ByteBuffer bin) throws TException {
        int length = bin.limit() - bin.position();
        this.writeI32(length);
        this.trans_.write(bin.array(), bin.position() + bin.arrayOffset(), length);
    }

    private KoalasTrace koalasTrace;

    public KoalasTrace getKoalasTrace() {
        return koalasTrace;
    }

    public void setKoalasTrace(KoalasTrace koalasTrace) {
        this.koalasTrace = koalasTrace;
    }

    public TMessage readMessageBegin() throws TException {
        int size = this.readI32();
        if (size < 0) {
            int version = size & -65536;
            if (version != -2147418112) {
                throw new TProtocolException(4, "Bad version in readMessageBegin");
            } else {
                TMessage tMessage = new TMessage(this.readString(), (byte)(size & 255), this.readI32());
                KoalasTrace koalasTrace = new KoalasTrace();
                koalasTrace.read ( this );
                setKoalasTrace(koalasTrace);
                return  tMessage;
            }
        } else if (this.strictRead_) {
            throw new TProtocolException(4, "Missing version in readMessageBegin, old client?");
        } else {
            TMessage tMessage =new TMessage(this.readStringBody(size), this.readByte(), this.readI32());
            KoalasTrace koalasTrace = new KoalasTrace();
            koalasTrace.read ( this );
            setKoalasTrace(koalasTrace);
            return tMessage;
        }
    }

    public void readMessageEnd() {
    }

    public TStruct readStructBegin() {
        return ANONYMOUS_STRUCT;
    }

    public void readStructEnd() {
    }

    public TField readFieldBegin() throws TException {
        byte type = this.readByte();
        short id = type == 0 ? 0 : this.readI16();
        return new TField("", type, id);
    }

    public void readFieldEnd() {
    }

    public TMap readMapBegin() throws TException {
        return new TMap(this.readByte(), this.readByte(), this.readI32());
    }

    public void readMapEnd() {
    }

    public TList readListBegin() throws TException {
        return new TList(this.readByte(), this.readI32());
    }

    public void readListEnd() {
    }

    public TSet readSetBegin() throws TException {
        return new TSet(this.readByte(), this.readI32());
    }

    public void readSetEnd() {
    }

    public boolean readBool() throws TException {
        return this.readByte() == 1;
    }

    public byte readByte() throws TException {
        if (this.trans_.getBytesRemainingInBuffer() >= 1) {
            byte b = this.trans_.getBuffer()[this.trans_.getBufferPosition()];
            this.trans_.consumeBuffer(1);
            return b;
        } else {
            this.readAll(this.bin, 0, 1);
            return this.bin[0];
        }
    }

    public short readI16() throws TException {
        byte[] buf = this.i16rd;
        int off = 0;
        if (this.trans_.getBytesRemainingInBuffer() >= 2) {
            buf = this.trans_.getBuffer();
            off = this.trans_.getBufferPosition();
            this.trans_.consumeBuffer(2);
        } else {
            this.readAll(this.i16rd, 0, 2);
        }

        return (short)((buf[off] & 255) << 8 | buf[off + 1] & 255);
    }

    public int readI32() throws TException {
        byte[] buf = this.i32rd;
        int off = 0;
        if (this.trans_.getBytesRemainingInBuffer() >= 4) {
            buf = this.trans_.getBuffer();
            off = this.trans_.getBufferPosition();
            this.trans_.consumeBuffer(4);
        } else {
            this.readAll(this.i32rd, 0, 4);
        }

        return (buf[off] & 255) << 24 | (buf[off + 1] & 255) << 16 | (buf[off + 2] & 255) << 8 | buf[off + 3] & 255;
    }

    public long readI64() throws TException {
        byte[] buf = this.i64rd;
        int off = 0;
        if (this.trans_.getBytesRemainingInBuffer() >= 8) {
            buf = this.trans_.getBuffer();
            off = this.trans_.getBufferPosition();
            this.trans_.consumeBuffer(8);
        } else {
            this.readAll(this.i64rd, 0, 8);
        }

        return (long)(buf[off] & 255) << 56 | (long)(buf[off + 1] & 255) << 48 | (long)(buf[off + 2] & 255) << 40 | (long)(buf[off + 3] & 255) << 32 | (long)(buf[off + 4] & 255) << 24 | (long)(buf[off + 5] & 255) << 16 | (long)(buf[off + 6] & 255) << 8 | (long)(buf[off + 7] & 255);
    }

    public double readDouble() throws TException {
        return Double.longBitsToDouble(this.readI64());
    }

    public String readString() throws TException {
        int size = this.readI32();
        if (this.trans_.getBytesRemainingInBuffer() >= size) {
            try {
                String s = new String(this.trans_.getBuffer(), this.trans_.getBufferPosition(), size, "UTF-8");
                this.trans_.consumeBuffer(size);
                return s;
            } catch (UnsupportedEncodingException var3) {
                throw new TException("JVM DOES NOT SUPPORT UTF-8");
            }
        } else {
            return this.readStringBody(size);
        }
    }

    public String readStringBody(int size) throws TException {
        try {
            this.checkReadLength(size);
            byte[] buf = new byte[size];
            this.trans_.readAll(buf, 0, size);
            return new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException var3) {
            throw new TException("JVM DOES NOT SUPPORT UTF-8");
        }
    }

    public ByteBuffer readBinary() throws TException {
        int size = this.readI32();
        this.checkReadLength(size);
        if (this.trans_.getBytesRemainingInBuffer() >= size) {
            ByteBuffer bb = ByteBuffer.wrap(this.trans_.getBuffer(), this.trans_.getBufferPosition(), size);
            this.trans_.consumeBuffer(size);
            return bb;
        } else {
            byte[] buf = new byte[size];
            this.trans_.readAll(buf, 0, size);
            return ByteBuffer.wrap(buf);
        }
    }

    private int readAll(byte[] buf, int off, int len) throws TException {
        this.checkReadLength(len);
        return this.trans_.readAll(buf, off, len);
    }

    public void setReadLength(int readLength) {
        this.readLength_ = readLength;
        this.checkReadLength_ = true;
    }

    protected void checkReadLength(int length) throws TException {
        if (length < 0) {
            throw new TException("Negative length: " + length);
        } else {
            if (this.checkReadLength_) {
                this.readLength_ -= length;
                if (this.readLength_ < 0) {
                    throw new TException("Message length exceeded: " + length);
                }
            }

        }
    }

    public static class Factory implements TProtocolFactory {
        protected boolean strictRead_;
        protected boolean strictWrite_;
        protected int readLength_;

        public Factory() {
            this(false, true);
        }

        public Factory(boolean strictRead, boolean strictWrite) {
            this(strictRead, strictWrite, 0);
        }

        public Factory(boolean strictRead, boolean strictWrite, int readLength) {
            this.strictRead_ = false;
            this.strictWrite_ = true;
            this.strictRead_ = strictRead;
            this.strictWrite_ = strictWrite;
            this.readLength_ = readLength;
        }

        public TProtocol getProtocol(TTransport trans) {
            KoalasBinaryProtocol proto = new KoalasBinaryProtocol (trans, this.strictRead_, this.strictWrite_);
            if (this.readLength_ != 0) {
                proto.setReadLength(this.readLength_);
            }

            return proto;
        }
    }
}
