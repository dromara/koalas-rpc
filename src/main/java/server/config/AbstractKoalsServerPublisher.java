package server.config;

import generic.GenericService;
import generic.GenericServiceImpl;
import io.netty.util.internal.SystemPropertyUtil;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import server.IkoalasServer;

import java.lang.reflect.InvocationTargetException;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class AbstractKoalsServerPublisher {
    private final static Logger logger = LoggerFactory.getLogger ( AbstractKoalsServerPublisher.class );
    public static final int DEFAULT_EVENT_LOOP_THREADS;
    public static final int DEFAULT_KOALAS_THREADS;
    public static final int DEFAULT_THRIFT_ACCETT_THREAD;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
        DEFAULT_KOALAS_THREADS=256;
        DEFAULT_THRIFT_ACCETT_THREAD=5;
    }

    public static final String IFACE = "Iface";
    public static final String PROCESSOR = "Processor";

    public Object serviceImpl;
    public Class<?> serviceInterface;
    public int port;
    public String zkpath;

    public int bossThreadCount;
    public int workThreadCount;
    public int koalasThreadCount;
    public String env="dev";
    public int weight=10;
    public String serverType="NETTY";
    public int workQueue;

    //RSA service
    public String privateKey;
    public String publicKey;

    public ApplicationContext applicationContext;
    public IkoalasServer ikoalasServer;

    public Object getServiceImpl() {
        return serviceImpl;
    }

    public void setServiceImpl(Object serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    public Class<?> getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class<?> serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreadCount() {
        return bossThreadCount;
    }

    public void setBossThreadCount(int bossThreadCount) {
        this.bossThreadCount = bossThreadCount;
    }

    public int getWorkThreadCount() {
        return workThreadCount;
    }

    public void setWorkThreadCount(int workThreadCount) {
        this.workThreadCount = workThreadCount;
    }

    public int getKoalasThreadCount() {
        return koalasThreadCount;
    }

    public void setKoalasThreadCount(int koalasThreadCount) {
        this.koalasThreadCount = koalasThreadCount;
    }

    public String getZkpath() {
        return zkpath;
    }

    public void setZkpath(String zkpath) {
        this.zkpath = zkpath;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public int getWorkQueue() {
        return workQueue;
    }

    public void setWorkQueue(int workQueue) {
        this.workQueue = workQueue;
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

    public TProcessor getTProcessor(){
        Class iface = getSynIfaceInterface(serviceInterface);
        try {
           return getProcessorClass(serviceInterface).getDeclaredConstructor (  iface).newInstance ( serviceImpl );
        } catch (NoSuchMethodException e) {
            logger.error ( "can't find the TProcessor Constructor with Iface",e );
        } catch (IllegalAccessException e) {
            logger.error ( "IllegalAccessException with Iface" );
        } catch (InstantiationException e) {
            logger.error ( "IllegalInstantiationExceptionAccessException with Iface",e );
        } catch (InvocationTargetException e) {
            logger.error ( "InvocationTargetException with Iface",e );
        }

        return null;
    }

    public TProcessor getGenericTProcessor(){
        Class iface = getSynIfaceInterface(GenericService.class);
        try {
            return getProcessorClass(GenericService.class).getDeclaredConstructor (  iface).newInstance ( new GenericServiceImpl ( serviceImpl ) );
        } catch (NoSuchMethodException e) {
            logger.error ( "can't find the GenericTProcessor Constructor with Iface",e );
        } catch (IllegalAccessException e) {
            logger.error ( "IllegalAccessException the GenericTProcessor with Iface" );
        } catch (InstantiationException e) {
            logger.error ( "IllegalInstantiationExceptionAccessException the GenericTProcessor with Iface",e );
        } catch (InvocationTargetException e) {
            logger.error ( "InvocationTargetException the GenericTProcessor with Iface",e );
        }

        return null;
    }

    private Class<?> getSynIfaceInterface(Class<?> serviceInterface) {
        Class<?>[] classes = serviceInterface.getClasses();
        for (Class c : classes)
            if (c.isMemberClass() && c.isInterface() && c.getSimpleName().equals( IFACE )) {
                return c;
            }
        throw new IllegalArgumentException("serviceInterface must contain Sub Interface of Iface");
    }

    private Class<TProcessor> getProcessorClass(Class<?> serviceInterface) {
        Class<?>[] classes = serviceInterface.getClasses();
        for (Class c : classes)
            if (c.isMemberClass() && !c.isInterface() && c.getSimpleName().equals( PROCESSOR )) {
                return c;
            }
        throw new IllegalArgumentException("serviceInterface must contain Sub Interface of Processor");
    }

    protected  void checkparam(){
        if(serviceImpl ==null){
            throw new IllegalArgumentException ( "the serviceImpl can't be null" );
        }
        if(serviceInterface ==null){
            throw new IllegalArgumentException ( "the serviceInterface can't be null" );
        }
        if(port ==0){
            throw new IllegalArgumentException ( "set the right port" );
        }

    }

    @Override
    public String toString() {
        return "KoalasServerPublisher{" +
                "serviceImpl=" + serviceImpl +
                ", serviceInterface=" + serviceInterface +
                ", port=" + port +
                ", zkpath='" + zkpath + '\'' +
                ", env='" + env + '\'' +
                ", serverType='" + serverType + '\'' +
                '}';
    }
}
