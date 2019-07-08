package client.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date: 2018年09月18日14:35:14
 */
public class LocalMockInterceptor implements MethodInterceptor {

    private String serviceImpl;

    public LocalMockInterceptor(String serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod ();
        Object[] arguments = invocation.getArguments();
        Object target = Class.forName(serviceImpl).newInstance();
        Object result = method.invoke(target, arguments);
        return result;
    }
}
