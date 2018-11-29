package thrift;

import thrift.KoalasAbstractNonblockingServer.FrameBuffer;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
class Invocation implements Runnable {
  private final FrameBuffer frameBuffer;

  public Invocation(final FrameBuffer frameBuffer) {
    this.frameBuffer = frameBuffer;
  }

  public void run() {
    frameBuffer.invoke();
  }
}