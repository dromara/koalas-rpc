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

import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.*;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalasThreadedSelectorServer extends KoalasAbstractNonblockingServer {
  private static final Logger LOGGER = LoggerFactory.getLogger( KoalasThreadedSelectorServer.class.getName());

  public static class Args extends AbstractNonblockingServerArgs<Args> {

    public int selectorThreads = 2;

    private int workerThreads = 5;
    private int stopTimeoutVal = 60;
    private TimeUnit stopTimeoutUnit = TimeUnit.SECONDS;
    private ExecutorService executorService = null;

    private int acceptQueueSizePerThread = 4;

    public static enum AcceptPolicy {

      FAIR_ACCEPT,

      FAST_ACCEPT
    }

    private AcceptPolicy acceptPolicy = AcceptPolicy.FAST_ACCEPT;

    public Args(TNonblockingServerTransport transport) {
      super(transport);
    }

    public Args selectorThreads(int i) {
      selectorThreads = i;
      return this;
    }

    public int getSelectorThreads() {
      return selectorThreads;
    }

    public Args workerThreads(int i) {
      workerThreads = i;
      return this;
    }

    public int getWorkerThreads() {
      return workerThreads;
    }

    public int getStopTimeoutVal() {
      return stopTimeoutVal;
    }

    public Args stopTimeoutVal(int stopTimeoutVal) {
      this.stopTimeoutVal = stopTimeoutVal;
      return this;
    }

    public TimeUnit getStopTimeoutUnit() {
      return stopTimeoutUnit;
    }

    public Args stopTimeoutUnit(TimeUnit stopTimeoutUnit) {
      this.stopTimeoutUnit = stopTimeoutUnit;
      return this;
    }

    public ExecutorService getExecutorService() {
      return executorService;
    }

    public Args executorService(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public int getAcceptQueueSizePerThread() {
      return acceptQueueSizePerThread;
    }

    public Args acceptQueueSizePerThread(int acceptQueueSizePerThread) {
      this.acceptQueueSizePerThread = acceptQueueSizePerThread;
      return this;
    }

    public AcceptPolicy getAcceptPolicy() {
      return acceptPolicy;
    }

    public Args acceptPolicy(AcceptPolicy acceptPolicy) {
      this.acceptPolicy = acceptPolicy;
      return this;
    }

    public void validate() {
      if (selectorThreads <= 0) {
        throw new IllegalArgumentException("selectorThreads must be positive.");
      }
      if (workerThreads < 0) {
        throw new IllegalArgumentException("workerThreads must be non-negative.");
      }
      if (acceptQueueSizePerThread <= 0) {
        throw new IllegalArgumentException("acceptQueueSizePerThread must be positive.");
      }
    }
  }

  private volatile boolean stopped_ = true;

  private AcceptThread acceptThread;

  private final Set<SelectorThread> selectorThreads = new HashSet<SelectorThread>();

  private final ExecutorService invoker;

  private final Args args;

  private String privateKey;
  private String publicKey;

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

  public KoalasThreadedSelectorServer(Args args) {
    super(args);
    args.validate();
    invoker = args.executorService == null ? createDefaultExecutor(args) : args.executorService;
    this.args = args;
  }

  @Override
  protected boolean startThreads() {
    try {
      for (int i = 0; i < args.selectorThreads; ++i) {
        selectorThreads.add(new SelectorThread(args.acceptQueueSizePerThread));
      }
      acceptThread = new AcceptThread((TNonblockingServerTransport) serverTransport_,
        createSelectorThreadLoadBalancer(selectorThreads));
      stopped_ = false;
      for (SelectorThread thread : selectorThreads) {
        thread.start();
      }
      acceptThread.start();
      return true;
    } catch (IOException e) {
      LOGGER.error("Failed to start threads!", e);
      return false;
    }
  }

  @Override
  protected void waitForShutdown() {
    try {
      joinThreads();
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted while joining threads!", e);
    }
    gracefullyShutdownInvokerPool();
  }

  protected void joinThreads() throws InterruptedException {
    acceptThread.join();
    for (SelectorThread thread : selectorThreads) {
      thread.join();
    }
  }

  @Override
  public void stop() {
    stopped_ = true;
    stopListening();

    if (acceptThread != null) {
      acceptThread.wakeupSelector();
    }
    if (selectorThreads != null) {
      for (SelectorThread thread : selectorThreads) {
        if (thread != null)
          thread.wakeupSelector();
      }
    }
  }

  protected void gracefullyShutdownInvokerPool() {
    invoker.shutdown();

    long timeoutMS = args.stopTimeoutUnit.toMillis(args.stopTimeoutVal);
    long now = System.currentTimeMillis();
    while (timeoutMS >= 0) {
      try {
        invoker.awaitTermination(timeoutMS, TimeUnit.MILLISECONDS);
        break;
      } catch (InterruptedException ix) {
        long newnow = System.currentTimeMillis();
        timeoutMS -= (newnow - now);
        now = newnow;
      }
    }
  }

  @Override
  protected boolean requestInvoke(FrameBuffer frameBuffer) {
    Runnable invocation = getRunnable(frameBuffer);
    if (invoker != null) {
      try {
        invoker.execute(invocation);
        return true;
      } catch (RejectedExecutionException rx) {
        LOGGER.warn("ExecutorService rejected execution!", rx);
        return false;
      }
    } else {
      invocation.run();
      return true;
    }
  }

  protected Runnable getRunnable(FrameBuffer frameBuffer) {
    return new Invocation(frameBuffer);
  }

  protected static ExecutorService createDefaultExecutor(Args options) {
    return (options.workerThreads > 0) ? Executors.newFixedThreadPool(options.workerThreads) : null;
  }

  private static BlockingQueue<TNonblockingTransport> createDefaultAcceptQueue(int queueSize) {
    if (queueSize == 0) {
      return new LinkedBlockingQueue<TNonblockingTransport>();
    }
    return new ArrayBlockingQueue<TNonblockingTransport>(queueSize);
  }


  protected class AcceptThread extends Thread {

    private final TNonblockingServerTransport serverTransport;
    private final Selector acceptSelector;

    private final SelectorThreadLoadBalancer threadChooser;

    public AcceptThread(TNonblockingServerTransport serverTransport,
        SelectorThreadLoadBalancer threadChooser) throws IOException {
      this.serverTransport = serverTransport;
      this.threadChooser = threadChooser;
      this.acceptSelector = SelectorProvider.provider().openSelector();
      this.serverTransport.registerSelector(acceptSelector);
    }

    public void run() {
      try {
        while (!stopped_) {
          select();
        }
      } catch (Throwable t) {
        LOGGER.error("run() exiting due to uncaught error", t);
      } finally {
        KoalasThreadedSelectorServer.this.stop();
      }
    }

    public void wakeupSelector() {
      acceptSelector.wakeup();
    }

    private void select() {
      try {
        acceptSelector.select();

        Iterator<SelectionKey> selectedKeys = acceptSelector.selectedKeys().iterator();
        while (!stopped_ && selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          if (!key.isValid()) {
            continue;
          }

          if (key.isAcceptable()) {
            handleAccept();
          } else {
            LOGGER.warn("Unexpected state in select! " + key.interestOps());
          }
        }
      } catch (IOException e) {
        LOGGER.warn("Got an IOException while selecting!", e);
      }
    }

    private void handleAccept() {
      final TNonblockingTransport client = doAccept();
      if (client != null) {
        final SelectorThread targetThread = threadChooser.nextThread();

        if (args.acceptPolicy == Args.AcceptPolicy.FAST_ACCEPT || invoker == null) {
          doAddAccept(targetThread, client);
        } else {
          try {
            invoker.submit(new Runnable() {
              public void run() {
                doAddAccept(targetThread, client);
              }
            });
          } catch (RejectedExecutionException rx) {
            LOGGER.warn("ExecutorService rejected accept registration!", rx);
            client.close();
          }
        }
      }
    }

    private TNonblockingTransport doAccept() {
      try {
        return (TNonblockingTransport) serverTransport.accept();
      } catch (TTransportException tte) {
        LOGGER.warn("Exception trying to accept!", tte);
        return null;
      }
    }

    private void doAddAccept(SelectorThread thread, TNonblockingTransport client) {
      if (!thread.addAcceptedConnection(client)) {
        client.close();
      }
    }
  }

  protected class SelectorThread extends AbstractSelectThread {

    private final BlockingQueue<TNonblockingTransport> acceptedQueue;

    public SelectorThread() throws IOException {
      this(new LinkedBlockingQueue<TNonblockingTransport>());
    }

    public SelectorThread(int maxPendingAccepts) throws IOException {
      this(createDefaultAcceptQueue(maxPendingAccepts));
    }

    public SelectorThread(BlockingQueue<TNonblockingTransport> acceptedQueue) throws IOException {
      this.acceptedQueue = acceptedQueue;
    }

    public boolean addAcceptedConnection(TNonblockingTransport accepted) {
      try {
        acceptedQueue.put(accepted);
      } catch (InterruptedException e) {
        LOGGER.warn("Interrupted while adding accepted connection!", e);
        return false;
      }
      selector.wakeup();
      return true;
    }

    public void run() {
      try {
        while (!stopped_) {
          select();
          processAcceptedConnections();
          processInterestChanges();
        }
        for (SelectionKey selectionKey : selector.keys()) {
          cleanupSelectionKey(selectionKey);
        }
      } catch (Throwable t) {
        LOGGER.error("run() exiting due to uncaught error", t);
      } finally {
        // This will wake up the accept thread and the other selector threads
        KoalasThreadedSelectorServer.this.stop();
      }
    }

    private void select() {
      try {
        selector.select();

        Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
        while (!stopped_ && selectedKeys.hasNext()) {
          SelectionKey key = selectedKeys.next();
          selectedKeys.remove();

          if (!key.isValid()) {
            cleanupSelectionKey(key);
            continue;
          }

          if (key.isReadable()) {
            handleRead(key);
          } else if (key.isWritable()) {
            handleWrite(key);
          } else {
            LOGGER.warn("Unexpected state in select! " + key.interestOps());
          }
        }
      } catch (IOException e) {
        LOGGER.warn("Got an IOException while selecting!", e);
      }
    }

    private void processAcceptedConnections() {
      // Register accepted connections
      while (!stopped_) {
        TNonblockingTransport accepted = acceptedQueue.poll();
        if (accepted == null) {
          break;
        }
        registerAccepted(accepted);
      }
    }

    private void registerAccepted(TNonblockingTransport accepted) {
      SelectionKey clientKey = null;
      try {
        clientKey = accepted.registerSelector(selector, SelectionKey.OP_READ);

        FrameBuffer frameBuffer = new FrameBuffer(accepted, clientKey, SelectorThread.this,privateKey,publicKey);
        clientKey.attach(frameBuffer);
      } catch (IOException e) {
        LOGGER.warn("Failed to register accepted connection to selector!", e);
        if (clientKey != null) {
          cleanupSelectionKey(clientKey);
        }
        accepted.close();
      }
    }
  }

  protected SelectorThreadLoadBalancer createSelectorThreadLoadBalancer(Collection<? extends SelectorThread> threads) {
    return new SelectorThreadLoadBalancer(threads);
  }

  protected class SelectorThreadLoadBalancer {
    private final Collection<? extends SelectorThread> threads;
    private Iterator<? extends SelectorThread> nextThreadIterator;

    public <T extends SelectorThread> SelectorThreadLoadBalancer(Collection<T> threads) {
      if (threads.isEmpty()) {
        throw new IllegalArgumentException("At least one selector thread is required");
      }
      this.threads = Collections.unmodifiableList(new ArrayList<T>(threads));
      nextThreadIterator = this.threads.iterator();
    }

    public SelectorThread nextThread() {
      if (!nextThreadIterator.hasNext()) {
        nextThreadIterator = threads.iterator();
      }
      return nextThreadIterator.next();
    }
  }
}
