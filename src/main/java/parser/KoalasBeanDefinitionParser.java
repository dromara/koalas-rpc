package parser;

import client.proxyfactory.KoalasClientProxy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import server.KoalasServerPublisher;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年12月20日11:23:01
 */
public class KoalasBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger ( KoalasBeanDefinitionParser.class );

    private Class<?> clazz;

    private AtomicInteger atomicInteger = new AtomicInteger (  );

     KoalasBeanDefinitionParser(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition ();
        beanDefinition.setBeanClass ( clazz );
        beanDefinition.setDestroyMethodName ( "destroy" );
        beanDefinition.setScope ( BeanDefinition.SCOPE_SINGLETON );

        String id = element.getAttribute ( "id" );
        if(StringUtils.isNotEmpty ( id )){
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            //beanDefinition.getPropertyValues().addPropertyValue("id", id);
        } else{
            parserContext.getRegistry().registerBeanDefinition(clazz.getName () + atomicInteger.getAndIncrement (), beanDefinition);
        }

        if (clazz == KoalasClientProxy.class) {
            String serviceInterface = element.getAttribute ( "serviceInterface" );
            try {
                beanDefinition.getPropertyValues ().addPropertyValue ( "serviceInterface",Class.forName ( serviceInterface ) );
            } catch (ClassNotFoundException e) {
                logger.error ( "this serviceInterface: "+serviceInterface+" is wrong",e );
            }

            String zkPath = element.getAttribute ( "zkPath" );
            if(!StringUtils.isEmpty ( zkPath )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "zkPath",zkPath);
            }

            String serverIpPorts = element.getAttribute ( "serverIpPorts" );
            if(!StringUtils.isEmpty ( serverIpPorts )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "serverIpPorts",serverIpPorts);
            }

            String connTimeout = element.getAttribute ( "connTimeout" );
            if(!StringUtils.isEmpty ( connTimeout )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "connTimeout",Integer.valueOf ( connTimeout ));
            }

            String readTimeout = element.getAttribute ( "readTimeout" );
            if(!StringUtils.isEmpty ( readTimeout )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "readTimeout",Integer.valueOf ( readTimeout ));
            }

            String localMockServiceImpl = element.getAttribute ( "localMockServiceImpl" );
            if(!StringUtils.isEmpty ( localMockServiceImpl )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "localMockServiceImpl",localMockServiceImpl);
            }

            String async = element.getAttribute ( "async" );
            if(!StringUtils.isEmpty ( async ) && "true".equals (async)){
                beanDefinition.getPropertyValues ().addPropertyValue ( "async",Boolean.valueOf ( async ));
            }
            String retryRequest = element.getAttribute ( "retryRequest" );
            if(!StringUtils.isEmpty ( retryRequest )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "retryRequest",Boolean.valueOf ( retryRequest ));
            }
            String retryTimes = element.getAttribute ( "retryTimes" );
            if(!StringUtils.isEmpty ( retryTimes )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "retryTimes",Integer.valueOf ( retryTimes ));
            }
            String maxTotal = element.getAttribute ( "maxTotal" );
            if(!StringUtils.isEmpty ( maxTotal )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "maxTotal",Integer.valueOf ( maxTotal ));
            }
            String maxIdle = element.getAttribute ( "maxIdle" );
            if(!StringUtils.isEmpty ( maxIdle )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "maxIdle",Integer.valueOf ( maxIdle ));
            }
            String minIdle = element.getAttribute ( "minIdle" );
            if(!StringUtils.isEmpty ( minIdle )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "minIdle",Integer.valueOf ( minIdle ));
            }
            String lifo = element.getAttribute ( "lifo" );
            if(!StringUtils.isEmpty ( lifo )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "lifo",Boolean.valueOf ( lifo ));
            }
            String fairness = element.getAttribute ( "fairness" );
            if(!StringUtils.isEmpty ( fairness )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "fairness",Boolean.valueOf ( fairness ));
            }
            String maxWaitMillis = element.getAttribute ( "maxWaitMillis" );
            if(!StringUtils.isEmpty ( maxWaitMillis )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "maxWaitMillis", Long.valueOf (maxWaitMillis));
            }
            String timeBetweenEvictionRunsMillis = element.getAttribute ( "timeBetweenEvictionRunsMillis" );
            if(!StringUtils.isEmpty ( timeBetweenEvictionRunsMillis )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "timeBetweenEvictionRunsMillis", Long.valueOf (timeBetweenEvictionRunsMillis));
            }
            String minEvictableIdleTimeMillis = element.getAttribute ( "minEvictableIdleTimeMillis" );
            if(!StringUtils.isEmpty ( minEvictableIdleTimeMillis )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "minEvictableIdleTimeMillis", Long.valueOf (minEvictableIdleTimeMillis));
            }
            String softMinEvictableIdleTimeMillis = element.getAttribute ( "softMinEvictableIdleTimeMillis" );
            if(!StringUtils.isEmpty ( softMinEvictableIdleTimeMillis )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "softMinEvictableIdleTimeMillis", Long.valueOf (softMinEvictableIdleTimeMillis));
            }

            String numTestsPerEvictionRun = element.getAttribute ( "numTestsPerEvictionRun" );
            if(!StringUtils.isEmpty ( numTestsPerEvictionRun )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "numTestsPerEvictionRun",Integer.valueOf ( numTestsPerEvictionRun ));
            }
            String testOnCreate = element.getAttribute ( "testOnCreate" );
            if(!StringUtils.isEmpty ( testOnCreate )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "testOnCreate",Boolean.valueOf ( testOnCreate ));
            }
            String testOnBorrow = element.getAttribute ( "testOnBorrow" );
            if(!StringUtils.isEmpty ( testOnBorrow )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "testOnBorrow",Boolean.valueOf ( testOnBorrow ));
            }
            String testOnReturn = element.getAttribute ( "testOnReturn" );
            if(!StringUtils.isEmpty ( testOnReturn )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "testOnReturn",Boolean.valueOf ( testOnReturn ));
            }
            String testWhileIdle = element.getAttribute ( "testWhileIdle" );
            if(!StringUtils.isEmpty ( testWhileIdle )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "testWhileIdle",Boolean.valueOf ( testWhileIdle ));
            }
            String iLoadBalancer = element.getAttribute ( "iLoadBalancer" );
            if(!StringUtils.isEmpty ( iLoadBalancer )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "iLoadBalancer",new RuntimeBeanReference(iLoadBalancer));
            }
            String env = element.getAttribute ( "env" );
            if(!StringUtils.isEmpty ( env )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "iLoadBalancer",env);
            }
            String removeAbandonedOnBorrow = element.getAttribute ( "removeAbandonedOnBorrow" );
            if(!StringUtils.isEmpty ( removeAbandonedOnBorrow )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "removeAbandonedOnBorrow",Boolean.valueOf ( removeAbandonedOnBorrow ));
            }
            String removeAbandonedOnMaintenance = element.getAttribute ( "removeAbandonedOnMaintenance" );
            if(!StringUtils.isEmpty ( removeAbandonedOnBorrow )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "removeAbandonedOnMaintenance",Boolean.valueOf ( removeAbandonedOnMaintenance ));
            }
            String removeAbandonedTimeout = element.getAttribute ( "removeAbandonedTimeout" );
            if(!StringUtils.isEmpty ( removeAbandonedTimeout )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "removeAbandonedTimeout",Integer.valueOf ( removeAbandonedTimeout ));
            }
            String maxLength_ = element.getAttribute ( "maxLength_" );
            if(!StringUtils.isEmpty ( maxLength_ )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "maxLength_",Integer.valueOf ( maxLength_ ));
            }
            String cores = element.getAttribute ( "cores" );
            if(!StringUtils.isEmpty ( cores )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "cores",Integer.valueOf ( cores ));
            }
            String asyncSelectorThreadCount = element.getAttribute ( "asyncSelectorThreadCount" );
            if(!StringUtils.isEmpty ( asyncSelectorThreadCount )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "asyncSelectorThreadCount",Integer.valueOf ( asyncSelectorThreadCount ));
            }
            String privateKey = element.getAttribute ( "privateKey" );
            if(!StringUtils.isEmpty ( privateKey )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "privateKey",privateKey);
            }
            String publicKey = element.getAttribute ( "publicKey" );
            if(!StringUtils.isEmpty ( publicKey )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "publicKey",publicKey);
            }
        } else if(clazz == KoalasServerPublisher.class){

            String serviceInterface = element.getAttribute ( "serviceInterface" );
            try {
                beanDefinition.getPropertyValues ().addPropertyValue ( "serviceInterface",Class.forName ( serviceInterface ) );
            } catch (ClassNotFoundException e) {
                logger.error ( "this serviceInterface: "+serviceInterface+" is wrong",e );
            }

            String serviceImpl = element.getAttribute ( "serviceImpl" );
            beanDefinition.getPropertyValues ().addPropertyValue ( "serviceImpl",new RuntimeBeanReference(serviceImpl));

            String port = element.getAttribute ( "port" );
            if(!StringUtils.isEmpty ( port )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "port",Integer.valueOf ( port ));
            }
            String zkpath = element.getAttribute ( "zkpath" );
            if(!StringUtils.isEmpty ( zkpath )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "zkpath",zkpath);
            }
            String bossThreadCount = element.getAttribute ( "bossThreadCount" );
            if(!StringUtils.isEmpty ( bossThreadCount )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "bossThreadCount",Integer.valueOf ( bossThreadCount ));
            }
            String workThreadCount = element.getAttribute ( "workThreadCount" );
            if(!StringUtils.isEmpty ( workThreadCount )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "workThreadCount",Integer.valueOf ( workThreadCount ));
            }
            String koalasThreadCount = element.getAttribute ( "koalasThreadCount" );
            if(!StringUtils.isEmpty ( koalasThreadCount )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "koalasThreadCount",Integer.valueOf ( koalasThreadCount ));
            }
            String env = element.getAttribute ( "env" );
            if(!StringUtils.isEmpty ( env )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "env",env);
            }
            String weight = element.getAttribute ( "weight" );
            if(!StringUtils.isEmpty ( weight )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "weight",Integer.valueOf ( weight ));
            }
            String serverType = element.getAttribute ( "serverType" );
            if(!StringUtils.isEmpty ( serverType )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "serverType",serverType);
            }
            String workQueue = element.getAttribute ( "workQueue" );
            if(!StringUtils.isEmpty ( workQueue )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "workQueue",Integer.valueOf ( workQueue ));
            }
            String privateKey = element.getAttribute ( "privateKey" );
            if(!StringUtils.isEmpty ( privateKey )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "privateKey",privateKey);
            }
            String publicKey = element.getAttribute ( "publicKey" );
            if(!StringUtils.isEmpty ( publicKey )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "publicKey",publicKey);
            }
        } else if(clazz==KoalasAnnotationBean.class){
            String KoalasPackage = element.getAttribute ( "package" );
            if(!StringUtils.isEmpty ( KoalasPackage )){
                beanDefinition.getPropertyValues ().addPropertyValue ( "annotationPackage",KoalasPackage);
            }
        }

        return beanDefinition;
    }
}
