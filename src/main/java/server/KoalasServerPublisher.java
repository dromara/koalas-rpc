package server;

import netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import server.config.AbstractKoalsServerPublisher;
import thrift.ThriftServer;

public class KoalasServerPublisher extends AbstractKoalsServerPublisher implements FactoryBean<Object>, ApplicationContextAware, InitializingBean {

    private final static Logger logger = LoggerFactory.getLogger ( KoalasServerPublisher.class );
    public static final String NETTY = "netty";
    public static final String THRIFT = "thrift";

    @Override
    public Object getObject() throws Exception {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return this.getClass ();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {

        this.checkparam();
        if(NETTY.equals ( this.serverType.toLowerCase ().trim () )){
            ikoalasServer = new NettyServer ( this );
        } else if(THRIFT.equals ( this.serverType.toLowerCase ().trim () )){
            ikoalasServer = new ThriftServer ( this );
        } else{
            throw  new IllegalArgumentException("other server is not support at since v1.0");
        }
        ikoalasServer.run ();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void destroy(){
        if(this.ikoalasServer != null){
            this.ikoalasServer.stop ();
        }
    }
}
