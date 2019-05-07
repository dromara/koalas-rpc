package parser;

import annotation.KoalasClient;
import annotation.KoalasServer;
import client.cluster.ILoadBalancer;
import client.proxyfactory.KoalasClientProxy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import server.KoalasServerPublisher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年12月20日11:23:01
 */
public class KoalasAnnotationBean implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, BeanFactoryAware, Ordered {

    public static final String COMMA_SPLIT_PATTERN = "\\s*[,]+\\s*";

    private String annotationPackage;

    private String[] annotationPackages;

    private BeanFactory beanFactory;

    private final Set<KoalasServerPublisher> koalasServerPublishers = new HashSet<> (  );
    private  final Map<String,KoalasClientProxy> koalasClientProxyMap = new ConcurrentHashMap<>();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        for (KoalasClientProxy koalasClientProxy:koalasClientProxyMap.values ()){
            koalasClientProxy.destroy ();
        }
        for (KoalasServerPublisher koalasServerPublisher:koalasServerPublishers){
            koalasServerPublisher.destroy ();
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (annotationPackage == null || annotationPackage.length () == 0) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        KoalasAnnotationBean.class.getClassLoader () );
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );
                // add filter
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        KoalasAnnotationBean.class.getClassLoader () );
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( KoalasServer.class );
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", KoalasAnnotationBean.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );
                // scan packages
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );
                scan.invoke ( scanner, new Object[]{annotationPackages} );
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage ( bean )) {
            return bean;
        }
        Class<?> clazz = bean.getClass ();
        if (isProxyBean ( bean )) {
            clazz = AopUtils.getTargetClass ( bean );
        }

        Method[] methods = clazz.getMethods ();

        for (Method method : methods) {
            String name = method.getName ();
            if (name.length () > 3 && name.startsWith ( "set" )
                    && method.getParameterTypes ().length == 1
                    && Modifier.isPublic ( method.getModifiers () )
                    && !Modifier.isStatic ( method.getModifiers () )) {
                try {
                    KoalasClient koalasClient = method.getAnnotation ( KoalasClient.class );
                    if (koalasClient != null) {
                        Object value = getClientInvoke ( koalasClient, method.getParameterTypes ()[0] );
                        if (value != null) {
                            method.invoke ( bean, new Object[]{value} );
                        }
                    }
                } catch (Exception e) {
                    throw new BeanInitializationException ( "Failed to init remote service reference at method " + name
                            + " in class " + bean.getClass ().getName (), e );
                }
            }
        }

        Field[] fields = clazz.getDeclaredFields ();
        for (Field field : fields) {
            try {
                if (!field.isAccessible ()) {
                    field.setAccessible ( true );
                }
                KoalasClient koalasClient = field.getAnnotation ( KoalasClient.class );
                if (koalasClient != null) {
                    Object value = getClientInvoke ( koalasClient, field.getType () );
                    if (value != null) {
                        field.set ( bean, value );
                    }
                }
            } catch (Exception e) {
                throw new BeanInitializationException ( "Failed to init remote service reference at filed " + field.getName ()
                        + " in class " + bean.getClass ().getName (), e );
            }
        }

        return bean;
    }

    private Object getClientInvoke(KoalasClient koalasClient, Class<?> beanClass) {

        KoalasClientProxy koalasClientProxy = new KoalasClientProxy ();

        boolean async;

        String interfaceName = beanClass.getName ();
        if (interfaceName.endsWith ( "$Iface" )) {
            koalasClientProxy.setAsync ( false );
            async=false;
        } else if (interfaceName.endsWith ( "$AsyncIface" )) {
            koalasClientProxy.setAsync ( true );
            async=true;
        } else {
            throw new RuntimeException ( "the bean :" + beanClass + "not allow with annotation @KoalasClient" );
        }

        if(StringUtils.isEmpty ( koalasClient.genericService () )){
            if(koalasClientProxyMap.containsKey (  interfaceName)){
                return koalasClientProxyMap.get ( interfaceName ).getObject ();
            }
        } else{
            if(koalasClientProxyMap.containsKey ( "generic-".concat ( async?"async-":"sync-" ).concat (koalasClient.genericService ()) )){
                return koalasClientProxyMap.get ( "generic-".concat ( async?"async-":"sync-" ).concat (koalasClient.genericService ())).getObject ();
            }
        }

        if(StringUtils.isNotBlank ( koalasClient.genericService () )){
            koalasClientProxy.setGeneric ( true );
            koalasClientProxy.setServiceInterface (koalasClient.genericService ());
        } else{
            koalasClientProxy.setGeneric ( false );
            koalasClientProxy.setServiceInterface ( beanClass.getDeclaringClass ().getName () );
        }


        if (StringUtils.isNotEmpty ( koalasClient.zkPath () )) {
            koalasClientProxy.setZkPath ( koalasClient.zkPath () );
        }

        if (StringUtils.isNotEmpty ( koalasClient.serverIpPorts () )) {
            koalasClientProxy.setServerIpPorts ( koalasClient.serverIpPorts () );
        }

        koalasClientProxy.setConnTimeout ( koalasClient.connTimeout () );
        koalasClientProxy.setReadTimeout ( koalasClient.readTimeout () );
        if (StringUtils.isNotEmpty ( koalasClient.localMockServiceImpl () )) {
            koalasClientProxy.setLocalMockServiceImpl (  koalasClient.localMockServiceImpl ());
        }

        koalasClientProxy.setRetryRequest ( koalasClient.retryRequest ());
        koalasClientProxy.setRetryTimes (  koalasClient.retryTimes ());
        koalasClientProxy.setMaxTotal (  koalasClient.maxTotal ());
        koalasClientProxy.setMaxIdle ( koalasClient.maxIdle () );
        koalasClientProxy.setMinIdle ( koalasClient.minIdle () );
        koalasClientProxy.setLifo (koalasClient.lifo ());
        koalasClientProxy.setFairness ( koalasClient.fairness () );
        koalasClientProxy.setMaxWaitMillis (koalasClient.maxWaitMillis ()  );
        koalasClientProxy.setTimeBetweenEvictionRunsMillis ( koalasClient.timeBetweenEvictionRunsMillis () );
        koalasClientProxy.setMinEvictableIdleTimeMillis ( koalasClient.minEvictableIdleTimeMillis () );
        koalasClientProxy.setSoftMinEvictableIdleTimeMillis ( koalasClient.softMinEvictableIdleTimeMillis () );
        koalasClientProxy.setNumTestsPerEvictionRun ( koalasClient.numTestsPerEvictionRun () );
        koalasClientProxy.setTestOnCreate ( koalasClient.testOnCreate () );
        koalasClientProxy.setTestOnBorrow ( koalasClient.testOnBorrow () );
        koalasClientProxy.setTestOnReturn ( koalasClient.testOnReturn () );
        koalasClientProxy.setTestWhileIdle ( koalasClient.testWhileIdle () );

        String iLoadBalancer = koalasClient.iLoadBalancer ();
        if(StringUtils.isNotEmpty (iLoadBalancer)){
            Object o = beanFactory.getBean ( iLoadBalancer );
            koalasClientProxy.setiLoadBalancer ( (ILoadBalancer) o );
        }
        koalasClientProxy.setEnv ( koalasClient.env () );
        koalasClientProxy.setRemoveAbandonedOnBorrow ( koalasClient.removeAbandonedOnBorrow () );
        koalasClientProxy.setRemoveAbandonedOnMaintenance ( koalasClient.removeAbandonedOnMaintenance () );
        koalasClientProxy.setRemoveAbandonedTimeout ( koalasClient.removeAbandonedTimeout () );
        koalasClientProxy.setMaxLength_ ( koalasClient.maxLength_ () );

        if(!StringUtils.isEmpty ( koalasClient.privateKey () ) && !StringUtils.isEmpty ( koalasClient.publicKey () )){
            koalasClientProxy.setPrivateKey (koalasClient.privateKey ()  );
            koalasClientProxy.setPublicKey (koalasClient.publicKey ()  );
        }
        koalasClientProxy.afterPropertiesSet ();
        if(StringUtils.isEmpty ( koalasClient.genericService () )){
            koalasClientProxyMap.put ( interfaceName, koalasClientProxy);
        } else{
            koalasClientProxyMap.put ( "generic-".concat ( async?"async-":"sync-" ).concat (koalasClient.genericService ()),koalasClientProxy );
        }

        return koalasClientProxy.getObject ();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Class<?> clazz = bean.getClass();
        if (isProxyBean(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        KoalasServer koalasServer = clazz.getAnnotation(KoalasServer.class);

        if(koalasServer != null){
            KoalasServerPublisher koalasServerPublisher = new KoalasServerPublisher ();
            koalasServerPublisher.setPort ( koalasServer.port () );
            koalasServerPublisher.setServiceImpl ( bean );

            if(!StringUtils.isEmpty ( koalasServer.zkpath () )){
                koalasServerPublisher.setZkpath ( koalasServer.zkpath () );
            }

            if(koalasServer.bossThreadCount () !=0){
                koalasServerPublisher.setBossThreadCount ( koalasServer.bossThreadCount () );
            }
            if(koalasServer.workThreadCount () !=0){
                koalasServerPublisher.setWorkThreadCount ( koalasServer.workThreadCount () );
            }
            if(koalasServer.koalasThreadCount () !=0){
                koalasServerPublisher.setKoalasThreadCount ( koalasServer.koalasThreadCount () );
            }
            koalasServerPublisher.setEnv ( koalasServer.env () );
            koalasServerPublisher.setWeight ( koalasServer.weight () );
            koalasServerPublisher.setServerType ( koalasServer.serverType () );
            if(koalasServer.workQueue () != 0){
                koalasServerPublisher.setWorkQueue ( koalasServer.workQueue () );
            }
            if(!StringUtils.isEmpty ( koalasServer.privateKey () ) && !StringUtils.isEmpty ( koalasServer.publicKey () )){
                koalasServerPublisher.setPrivateKey (koalasServer.privateKey ()  );
                koalasServerPublisher.setPublicKey (koalasServer.publicKey ()  );
            }
            Class<?> serviceinterface=null;
            Class<?>[] serviceinterfaces =  bean.getClass ().getInterfaces ();
            for(Class<?> clazz1:serviceinterfaces ){
                if(clazz1.getName ().endsWith ("$Iface")){
                    serviceinterface = clazz1.getDeclaringClass ();
                    break;
                }
            }

            if(serviceinterface == null){
                throw new BeanInitializationException ( "bean :" +bean.getClass ().getName () + "is not the thrift interface parent class");
            }
            koalasServerPublisher.setServiceInterface ( serviceinterface );
            koalasServerPublisher.afterPropertiesSet ();
            koalasServerPublishers.add (koalasServerPublisher  );
        }

        return bean;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public String getAnnotationPackage() {
        return annotationPackage;
    }

    public void setAnnotationPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length () == 0) ? null
                : annotationPackage.split ( COMMA_SPLIT_PATTERN );
    }

    private boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        Class clazz = bean.getClass ();
        if (isProxyBean ( bean )) {
            clazz = AopUtils.getTargetClass ( bean );
        }
        String beanClassName = clazz.getName ();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith ( pkg )) {
                return true;
            }
        }
        return false;
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy ( bean );
    }

}
