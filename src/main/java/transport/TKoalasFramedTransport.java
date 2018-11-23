package transport;

import org.apache.thrift.TByteArrayOutputStream;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;

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

  private final TByteArrayOutputStream writeBuffer_ = new TByteArrayOutputStream(1024);

  private TMemoryInputTransport readBuffer_ = new TMemoryInputTransport(new byte[0]);

  public static final byte first = (byte) 0xAB;
  public static final byte second = (byte) 0xBA;
  private byte version = (byte) 1;
  private byte heartbeat = (byte) 1;
  private byte rsa = (byte) 0;
  private byte zip = (byte) 0;
  private boolean ifUserProtocol=true;
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

    public Factory() {
      maxLength_ = TKoalasFramedTransport.DEFAULT_MAX_LENGTH;
    }

    public Factory(int maxLength) {
      maxLength_ = maxLength;
    }

    @Override
    public TTransport getTransport(TTransport base) {
      return new TKoalasFramedTransport (base, maxLength_);
    }

    public TTransport getTransport(TTransport base,boolean user) {
      return new TKoalasFramedTransport (base, maxLength_,user);
    }

  }

  public TKoalasFramedTransport(byte version, byte heartbeat, byte rsa, byte zip) {
    this.version = version;
    this.heartbeat = heartbeat;
    this.rsa = rsa;
    this.zip = zip;
  }

  public TKoalasFramedTransport(TTransport transport, int maxLength, boolean ifUserProtocol) {
    this((byte) 1,(byte) 1,(byte) 0,(byte) 0);
    transport_ = transport;
    maxLength_ = maxLength;
    this.ifUserProtocol = ifUserProtocol;
  }

  public TKoalasFramedTransport(TTransport transport, int maxLength) {
    this((byte) 1,(byte) 1,(byte) 0,(byte) 0);
    transport_ = transport;
    maxLength_ = maxLength;
  }

  public TKoalasFramedTransport(TTransport transport) {
    this((byte) 1,(byte) 1,(byte) 0,(byte) 0);
    transport_ = transport;
    maxLength_ = TKoalasFramedTransport.DEFAULT_MAX_LENGTH;
  }

  public void open() throws TTransportException {
    transport_.open();
  }

  public boolean isOpen() {
    return transport_.isOpen();
  }

  public void close() {
    transport_.close();
  }

  public int read(byte[] buf, int off, int len) throws TTransportException {
    if (readBuffer_ != null) {
      int got = readBuffer_.read(buf, off, len);
      if (got > 0) {
        return got;
      }
    }

    // Read another frame of data
    readFrame();

    return readBuffer_.read(buf, off, len);
  }

  @Override
  public byte[] getBuffer() {
    return readBuffer_.getBuffer();
  }

  @Override
  public int getBufferPosition() {
    return readBuffer_.getBufferPosition();
  }

  @Override
  public int getBytesRemainingInBuffer() {
    return readBuffer_.getBytesRemainingInBuffer();
  }

  @Override
  public void consumeBuffer(int len) {
    readBuffer_.consumeBuffer(len);
  }

  private final byte[] i32buf = new byte[4];

  private void readFrame() throws TTransportException {
    transport_.readAll(i32buf, 0, 4);
    int size = decodeFrameSize(i32buf);

    if (size < 0) {
      throw new TTransportException("Read a negative frame size (" + size + ")!");
    }

    if (size > maxLength_) {
      throw new TTransportException("Frame size (" + size + ") larger than max length (" + maxLength_ + ")!");
    }

    byte[] buff = new byte[size];
    transport_.readAll(buff, 0, size);

    if(buff[0]==first && buff[1]==second){
      ifUserProtocol = true;
    }else{
      ifUserProtocol = false;
    }

    if(ifUserProtocol){
      byte[] request = new byte[size - 6];
      System.arraycopy (buff,6,request,0,size-6);
      readBuffer_.reset(request);
      return;
    }

    readBuffer_.reset(buff);
  }

  public void write(byte[] buf, int off, int len) throws TTransportException {
    writeBuffer_.write(buf, off, len);
  }

  @Override
  public void flush() throws TTransportException {
    byte[] buf = writeBuffer_.get();
    int len = writeBuffer_.len();
    int thrift =len;
    writeBuffer_.reset();
    if(ifUserProtocol){
      len = len+6;
    }
    encodeFrameSize(len, i32buf);
    transport_.write(i32buf, 0, 4);

    //the 2.0 version will be add Ras and zip
    if(ifUserProtocol){
      transport_.write ( new byte[]{first} );
      transport_.write ( new byte[]{second} );
      transport_.write ( new byte[]{version} );
      transport_.write ( new byte[]{heartbeat} );
      transport_.write ( new byte[]{rsa} );
      transport_.write ( new byte[]{zip} );
    }

    transport_.write(buf, 0, thrift);
    transport_.flush();
  }

  public static final void encodeFrameSize(final int frameSize, final byte[] buf) {
    buf[0] = (byte)(0xff & (frameSize >> 24));
    buf[1] = (byte)(0xff & (frameSize >> 16));
    buf[2] = (byte)(0xff & (frameSize >> 8));
    buf[3] = (byte)(0xff & (frameSize));
  }

  public static final int decodeFrameSize(final byte[] buf) {
    return
      ((buf[0] & 0xff) << 24) |
      ((buf[1] & 0xff) << 16) |
      ((buf[2] & 0xff) <<  8) |
      ((buf[3] & 0xff));
  }
}
