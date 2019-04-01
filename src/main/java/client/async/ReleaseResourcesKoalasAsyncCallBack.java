package client.async;

import client.cluster.ServerObject;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:44:58
 */
public class ReleaseResourcesKoalasAsyncCallBack<T> implements AsyncMethodCallback<T> {

    private final static Logger logger = LoggerFactory.getLogger ( ReleaseResourcesKoalasAsyncCallBack.class );

    private AsyncMethodCallback<T> asyncMethodCallback;
    private ServerObject serverObject;
    private TTransport socket;

    public ReleaseResourcesKoalasAsyncCallBack(AsyncMethodCallback<T> asyncMethodCallback, ServerObject serverObject, TTransport socket) {
        this.asyncMethodCallback = asyncMethodCallback;
        this.serverObject = serverObject;
        this.socket = socket;
    }

    @Override
    public void onComplete(T t) {
        Method m;
        Object o;
        try {
            m = t.getClass ().getDeclaredMethod ( "getResult" );
            o = m.invoke ( t );
            serverObject.getGenericObjectPool ().returnObject ( socket );
            if (asyncMethodCallback != null)
                if (asyncMethodCallback instanceof KoalasAsyncCallBack) {
                    ((KoalasAsyncCallBack) asyncMethodCallback).onCompleteWithoutReflect ( o );
                } else {
                    asyncMethodCallback.onComplete ( t );
                }
        } catch (Exception e) {
            if (e instanceof InvocationTargetException && e.getCause () instanceof TApplicationException
                    && ((TApplicationException) e.getCause ()).getType () == TApplicationException.MISSING_RESULT) {
                if (asyncMethodCallback != null) {
                    if (asyncMethodCallback instanceof KoalasAsyncCallBack) {
                        ((KoalasAsyncCallBack) asyncMethodCallback).onCompleteWithoutReflect ( null );
                    } else {
                        asyncMethodCallback.onComplete ( t );
                    }
                }
                try {
                    serverObject.getGenericObjectPool ().returnObject ( socket );
                    return;
                } catch (Exception e1) {
                    logger.error ( "onComplete invalidateObject object error !", e );
                }
            } else{
                asyncMethodCallback.onError ( e );
            }
            try {
                serverObject.getGenericObjectPool ().invalidateObject ( socket );
            } catch (Exception e1) {
                logger.error ( "onComplete invalidateObject object error !", e );
            }
        }
    }

    @Override
    public void onError(Exception e) {
        try {
            serverObject.getGenericObjectPool ().invalidateObject ( socket );
        } catch (Exception e1) {
            logger.error ( "onError invalidateObject object error !", e );
        }
        asyncMethodCallback.onError ( e );
    }

}
