package server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServerRun2 {

    public static void main(String[] args) {

        ApplicationContext ac = new ClassPathXmlApplicationContext ("classpath:server/koalas-server2.xml");

        ((ClassPathXmlApplicationContext) ac).registerShutdownHook ();

    }
}
