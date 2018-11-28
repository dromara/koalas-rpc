package thrift;

import thrift.KoalasAbstractNonblockingServer.FrameBuffer;

class Invocation implements Runnable {
  private final FrameBuffer frameBuffer;

  public Invocation(final FrameBuffer frameBuffer) {
    this.frameBuffer = frameBuffer;
  }

  public void run() {
    frameBuffer.invoke();
  }
}