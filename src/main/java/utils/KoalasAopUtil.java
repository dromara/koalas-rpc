package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2019年06月19日21:22:39
 */
public class KoalasAopUtil {
    private final static Logger logger = LoggerFactory.getLogger ( KoalasAopUtil.class );

    /**
     * 获取被代理类的Object
     * @author Monkey
     */
    public static Object getTarget(Object proxy){

        try {
            if(!AopUtils.isAopProxy(proxy)) {
                //不是代理对象
                return proxy;
            }

            if(AopUtils.isJdkDynamicProxy(proxy)) {
                return getJdkDynamicProxyTargetObject(proxy);
            } else { //cglib
                return getCglibProxyTargetObject(proxy);
            }
        } catch (Exception e){
            logger.error (  "get spring target bean error,proxy="+proxy,e);
            return null;
        }

    }

    /**
     * CGLIB方式被代理类的获取
     * @author Monkey
     *
     */
    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }

    /**
     * JDK动态代理方式被代理类的获取
     * @author Monkey
     *
     */
    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
        return target;
    }

}
