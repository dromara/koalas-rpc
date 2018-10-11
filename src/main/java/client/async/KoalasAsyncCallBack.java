package client.async;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

public class KoalasAsyncCallBack<R, T> implements AsyncMethodCallback<T> {

    private final static Logger logger = LoggerFactory.getLogger ( KoalasAsyncCallBack.class );

    private R r;
    private Throwable e;
    private final CountDownLatch finished = new CountDownLatch ( 1 );

    private Future<R> future = new Future<R> () {

        private volatile boolean cancelled = false;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (finished.getCount() > 0) {
                cancelled = true;
                finished.countDown();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return finished.getCount() == 0;
        }

        @Override
        public R get() throws InterruptedException, ExecutionException {
            finished.await ();
            return getValue();
        }

        @Override
        public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if(finished.await ( timeout,unit )){
                return getValue();
            } else{
                throw  new TimeoutException("KoalasAsync TimeoutException");
            }
        }

        private  R getValue () throws ExecutionException, CancellationException {
            if (e != null) {
                throw new ExecutionException("Observer onFailure", e);
            } else if (cancelled) {
                throw new CancellationException("Subscriber unsubscribed");
            } else {
                return r;
            }
        }
    };

    public  Future<R> getFuture(){
        return future;
    }

    @Override
    public void onComplete(T response) {
        Method m;
        Object o;
        try {
            m = response.getClass ().getDeclaredMethod ( "getResult" );
            o = m.invoke ( response );
            r = (R) o;
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause () instanceof TApplicationException
                    && ((TApplicationException) e.getCause ()).getType () == TApplicationException.MISSING_RESULT) {
                r = null;
            } else {
                onError ( e );
                return;
            }
        }
        finished.countDown ();
    }

    public void onCompleteWithoutReflect(Object o) {
        r= (R) o;
        finished.countDown ();
    }

    @Override
    public void onError(Exception exception) {
        logger.error ( "the koalas Async faid!", exception );
        e=exception;
        finished.countDown ();
    }
}
