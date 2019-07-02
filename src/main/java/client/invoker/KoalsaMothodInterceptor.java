package client.invoker;

import client.async.ReleaseResourcesKoalasAsyncCallBack;
import client.cluster.Icluster;
import client.cluster.ServerObject;
import client.proxyfactory.KoalasClientProxy;
import com.dianping.cat.Cat;
import com.dianping.cat.CatConstants;
import com.dianping.cat.message.Event;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import exceptions.OutMaxLengthException;
import exceptions.RSAException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.KoalasTrace;
import utils.TraceThreadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalsaMothodInterceptor implements MethodInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger ( KoalsaMothodInterceptor.class );

    private Icluster icluster;
    private int retryTimes;
    private boolean retryRequest;
    private KoalasClientProxy koalasClientProxy;
    private int asyncTimeOut;
    private boolean cat;

    public KoalsaMothodInterceptor(Icluster icluster, int retryTimes, boolean retryRequest, KoalasClientProxy koalasClientProxy, int asyncTimeOut) {
        this.icluster = icluster;
        this.retryTimes = retryTimes;
        this.retryRequest = retryRequest;
        this.koalasClientProxy = koalasClientProxy;
        this.asyncTimeOut = asyncTimeOut;
        this.cat=koalasClientProxy.isCat ();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        Method method = invocation.getMethod ();
        String methodName = method.getName ();
        Object[] args = invocation.getArguments ();

        Class<?>[] parameterTypes = method.getParameterTypes ();
        if (method.getDeclaringClass () == Object.class) {
            try {
                return method.invoke ( this, args );
            } catch (IllegalAccessException e) {
                LOG.error ( e.getMessage (), e );
                return null;
            }
        }
        if ("toString".equals ( methodName ) && parameterTypes.length == 0) {
            return this.toString ();
        }
        if ("hashCode".equals ( methodName ) && parameterTypes.length == 0) {
            return this.hashCode ();
        }
        if ("equals".equals ( methodName ) && parameterTypes.length == 1) {
            return this.equals ( args[0] );
        }

        boolean serviceTop =false;

        Transaction transaction=null;

        if(cat){
            if(TraceThreadContext.get () ==null){
                serviceTop=true;
                transaction = Cat.newTransaction("Service", method.getDeclaringClass ().getName ().concat ( "." ).concat ( methodName ).concat ( ".top" ));

                MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
                String messageId = tree.getMessageId();

                if (messageId == null) {
                    messageId = Cat.createMessageId();
                    tree.setMessageId(messageId);
                }

                String childId = Cat.getProducer().createRpcServerId("default");

                String root = tree.getRootMessageId();

                if (root == null) {
                    root = messageId;
                }
                Cat.logEvent(CatConstants.TYPE_REMOTE_CALL, "", Event.SUCCESS, childId);

                KoalasTrace koalasTrace = new KoalasTrace (  );
                koalasTrace.setChildId (childId  );
                koalasTrace.setParentId (  messageId);
                koalasTrace.setRootId ( root );
                TraceThreadContext.set (koalasTrace);
            } else{
                KoalasTrace currentKoalasTrace = TraceThreadContext.get ();
                String child_Id = Cat.getProducer().createRpcServerId("default");
                Cat.logEvent(CatConstants.TYPE_REMOTE_CALL, "", Event.SUCCESS, child_Id);
                currentKoalasTrace.setChildId ( child_Id );
            }
        }


        try {
            TTransport socket = null;
            int currTryTimes = 0;
            while (currTryTimes++ < retryTimes) {
                ServerObject serverObject = icluster.getObjectForRemote ();
                if (serverObject == null) throw new IllegalStateException("no server list to use :" + koalasClientProxy.getServiceInterface ());
                GenericObjectPool<TTransport> genericObjectPool = serverObject.getGenericObjectPool ();
                try {
                    long before = System.currentTimeMillis ();
                    socket = genericObjectPool.borrowObject ();
                    long after = System.currentTimeMillis ();
                    LOG.debug ( "get Object from pool with {} ms", after - before );
                } catch (Exception e) {
                    if (socket != null)
                        genericObjectPool.returnObject ( socket );
                    LOG.error ( e.getMessage (), e );
                    if(transaction!=null && cat)
                        transaction.setStatus ( e );
                    throw new IllegalStateException("borrowObject error :" + koalasClientProxy.getServiceInterface ());
                }

                Object obj = koalasClientProxy.getInterfaceClientInstance ( socket, serverObject.getRemoteServer ().getServer () );

                if (obj instanceof TAsyncClient) {
                    ((TAsyncClient) obj).setTimeout ( asyncTimeOut );
                    if (args.length < 1) {
                        genericObjectPool.returnObject ( socket );
                        throw new IllegalStateException ( "args number error" );
                    }

                    Object argslast = args[args.length - 1];
                    if (!(argslast instanceof AsyncMethodCallback)) {
                        genericObjectPool.returnObject ( socket );
                        throw new IllegalStateException ( "args type error" );
                    }

                    AsyncMethodCallback callback = (AsyncMethodCallback) argslast;
                    ReleaseResourcesKoalasAsyncCallBack releaseResourcesKoalasAsyncCallBack = new ReleaseResourcesKoalasAsyncCallBack ( callback, serverObject, socket );
                    args[args.length - 1] = releaseResourcesKoalasAsyncCallBack;

                }
                try {
                    Object o = method.invoke ( obj, args );
                    if (socket instanceof TSocket) {
                        genericObjectPool.returnObject ( socket );

                    }
                    if(transaction!=null&& cat)
                        transaction.setStatus ( Transaction.SUCCESS );
                    return o;
                } catch (Exception e) {
                    Throwable cause = (e.getCause () == null) ? e : e.getCause ();

                    boolean ifreturn = false;
                    if (cause instanceof TApplicationException) {
                        if (((TApplicationException) cause).getType () == 6666) {
                            LOG.info ( "the server{}--serverName【{}】 thread pool is busy ,retry it!", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface () );
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                                ifreturn = true;
                            }
                            Thread.yield ();
                            if (retryRequest)
                                continue;
                        }

                        if (((TApplicationException) cause).getType () == 9999) {
                            LOG.error ( "rsa error with service--{}--serverName【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface ());
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            if(transaction!=null&& cat)
                                transaction.setStatus ( cause );
                            throw new RSAException ("rsa error with service" + serverObject.getRemoteServer ().toString ()+koalasClientProxy.getServiceInterface ());
                        }

                        if (((TApplicationException) cause).getType () == 6699) {
                            LOG.error ( "this client is not rsa support,please get the privateKey and publickey with server--{}--serverName【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface ());
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            if(transaction!=null&& cat)
                                transaction.setStatus ( cause );
                            throw new RSAException("this client is not rsa support,please get the privateKey and publickey with server" + serverObject.getRemoteServer ().toString ()+koalasClientProxy.getServiceInterface ());
                        }

                        if (((TApplicationException) cause).getType () == TApplicationException.INTERNAL_ERROR) {
                            LOG.error ( "this server is error, server--{}--serverName【{}】,the remote server error message data【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface (),((TApplicationException) cause).getMessage () );
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            if(transaction!=null&& cat)
                                transaction.setStatus ( cause );
                            throw new TApplicationException("this server is error serviceName:" + serverObject.getRemoteServer ()+koalasClientProxy.getServiceInterface () + ",error message:" + ((TApplicationException) cause).getMessage ());
                        }

                        if (((TApplicationException) cause).getType () == TApplicationException.MISSING_RESULT) {
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                            }
                            return null;
                        }
                    }

                    if (cause instanceof RSAException) {
                        LOG.error ( "this client privateKey or publicKey is error,please check it! --{}--serverName【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface () );
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        if(transaction!=null&& cat)
                            transaction.setStatus ( cause );
                        throw new RSAException("this client privateKey or publicKey is error,please check it!" + serverObject.getRemoteServer ()+ koalasClientProxy.getServiceInterface ());
                    }

                    if(cause instanceof OutMaxLengthException){
                        LOG.error ( (cause ).getMessage (),cause );
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        if(transaction!=null&& cat)
                            transaction.setStatus ( cause );
                        throw new OutMaxLengthException("to big content!" + serverObject.getRemoteServer ()+ koalasClientProxy.getServiceInterface ());
                    }

                    if (cause.getCause () != null && cause.getCause () instanceof ConnectException) {
                        LOG.error ( "the server {}--serverName【{}】 maybe is shutdown ,retry it!", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface () );
                        try {
                            if (socket != null) {
                                genericObjectPool.returnObject ( socket );
                                ifreturn = true;
                            }

                            if (retryRequest)
                                continue;
                        } catch (Exception e1) {
                            LOG.error ( "invalidateObject error!", e1 );
                        }
                    }

                    if (cause.getCause () != null && cause.getCause () instanceof SocketTimeoutException) {
                        LOG.error ( "read timeout SocketTimeoutException,retry it! {}--serverName【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface () );
                        if (socket != null) {
                            try {
                                genericObjectPool.invalidateObject ( socket );
                                ifreturn = true;
                            } catch (Exception e1) {
                                LOG.error ( "invalidateObject error ,", e );
                                if(transaction!=null&& cat)
                                    transaction.setStatus ( e1 );
                                throw new IllegalStateException("SocketTimeout and invalidateObject error" + serverObject.getRemoteServer () + koalasClientProxy.getServiceInterface ());
                            }
                        }
                        if (retryRequest)
                            continue;
                    }

                    if(cause instanceof TTransportException){
                        if(((TTransportException) cause).getType () == TTransportException.END_OF_FILE){
                            LOG.error ( "TTransportException,END_OF_FILE! {}--serverName【{}】", serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface ());
                            if (socket != null) {
                                try {
                                    genericObjectPool.invalidateObject ( socket );
                                } catch (Exception e1) {
                                    LOG.error ( "invalidateObject error", e );
                                    if(transaction!=null&& cat)
                                    transaction.setStatus ( e1 );
                                    throw new IllegalStateException("TTransportException and invalidateObject error" + serverObject.getRemoteServer () + koalasClientProxy.getServiceInterface ());
                                }
                            }
                            if(transaction!=null&& cat)
                            transaction.setStatus ( cause );
                            throw new TTransportException("the remote server is shutdown!" + serverObject.getRemoteServer () + koalasClientProxy.getServiceInterface ());
                        }
                        if(cause.getCause ()!=null && cause.getCause () instanceof SocketException){
                            if(genericObjectPool.isClosed ()){
                                LOG.warn ( "serverObject {} is close!,retry it",serverObject );
                                if (retryRequest)
                                    continue;
                            }
                        }
                    }

                    if(cause instanceof TBase){
                        LOG.warn ( "user exception--{}, {}--serverName【{}】",cause.getClass ().getName (), serverObject.getRemoteServer (),koalasClientProxy.getServiceInterface ());
                        if (socket != null) {
                            genericObjectPool.returnObject ( socket );
                        }
                        if(transaction!=null&& cat)
                            transaction.setStatus ( cause );
                        throw cause;
                    }

                    if (socket != null && !ifreturn)
                        genericObjectPool.returnObject ( socket );
                    LOG.error ( "invoke server error,server ip -【{}】,port -【{}】--serverName【{}】", serverObject.getRemoteServer ().getIp (), serverObject.getRemoteServer ().getPort (),koalasClientProxy.getServiceInterface ()  );
                    if(transaction!=null&& cat)
                    transaction.setStatus ( cause );
                    throw cause;
                }
            }
            IllegalStateException finallyException = new IllegalStateException("error!retry time-out of:" + retryTimes + "!!! " + koalasClientProxy.getServiceInterface ());
            if(transaction!=null&& cat)
                  transaction.setStatus ( finallyException );
            throw finallyException;
        } finally {
            if(transaction!=null&& cat)
                transaction.complete ();
            if(serviceTop && cat)
                TraceThreadContext.remove ();
        }
    }

}
