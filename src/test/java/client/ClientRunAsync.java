package client;

import org.apache.thrift.TException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import thrift.client.impl.TestServiceAsync;

public class ClientRunAsync {

    public static void main(String[] args) throws TException, InterruptedException {

        ApplicationContext ac = new ClassPathXmlApplicationContext ("classpath:client/koalas-client.xml");

        TestServiceAsync testServiceAsync  = (TestServiceAsync) ac.getBean ( "testServiceAsync" );

        for (int i = 0; i <1 ; i++) {
            try {
                testServiceAsync.getRemoteRpc ();
              }catch (Exception e){
                e.printStackTrace ();
            }
        }
        ((ClassPathXmlApplicationContext) ac).registerShutdownHook ();
     }
}
