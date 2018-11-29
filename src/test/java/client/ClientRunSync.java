package client;

import org.apache.thrift.TException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import thrift.client.impl.TestServiceSync;

public class ClientRunSync {

    public static void main(String[] args) throws TException, InterruptedException {

        ApplicationContext ac = new ClassPathXmlApplicationContext ("classpath:client/koalas-client.xml");

        TestServiceSync testServiceSync  = (TestServiceSync) ac.getBean ( "testServiceSync" );
        for (int i = 0; i < 1; i++) {
            try {
                testServiceSync.getRemoteRpc ();
            }catch (Exception e){
                e.printStackTrace ();
            }

        }
        ((ClassPathXmlApplicationContext) ac).registerShutdownHook ();

    }
}
