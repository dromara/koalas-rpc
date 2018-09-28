package utils;

import server.KoalasDefaultThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MTTThreadedSelectorWorkerExcutorUtil {

    public static ThreadPoolExecutor getWorkerExcutor(int threadcount, KoalasDefaultThreadFactory koalasDefaultThreadFactory) {
        return new ThreadPoolExecutor(threadcount, threadcount, 30L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable> (), koalasDefaultThreadFactory);
    }

    public static ThreadPoolExecutor getWorkerExecutorWithQueue(int min, int max, int workQueueSize, KoalasDefaultThreadFactory koalasDefaultThreadFactory) {
        ThreadPoolExecutor executor;
        if (workQueueSize <= 0) {
            executor = new ThreadPoolExecutor(min, max, 30L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), koalasDefaultThreadFactory);
        } else {
            executor = new ThreadPoolExecutor(min, max, 30L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable> (workQueueSize), koalasDefaultThreadFactory);
        }
        executor.prestartAllCoreThreads();
        return executor;
    }
}