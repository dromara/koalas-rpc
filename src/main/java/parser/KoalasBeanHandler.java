package parser;

import client.proxyfactory.KoalasClientProxy;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import server.KoalasServerPublisher;

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
