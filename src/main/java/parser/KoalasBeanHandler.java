package parser;

import client.proxyfactory.KoalasClientProxy;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import server.KoalasServerPublisher;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年12月20日11:23:01
 */
public class KoalasBeanHandler extends NamespaceHandlerSupport {

    public static final String CLIENT = "client";
    public static final String SERVER = "server";
    public static final String ANNOTATION = "annotation";

    @Override
    public void init() {
        registerBeanDefinitionParser( CLIENT,new KoalasBeanDefinitionParser (KoalasClientProxy.class));
        registerBeanDefinitionParser( SERVER,new KoalasBeanDefinitionParser ( KoalasServerPublisher.class));
        registerBeanDefinitionParser( ANNOTATION,new KoalasBeanDefinitionParser ( KoalasAnnotationBean.class));
    }
}
