package xml.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import thrift.xml.client.impl.TestServiceAsync;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:xml/client/koalas-client.xml"})
public class ClientRunAsync {

     @Autowired
     TestServiceAsync testServiceAsync;

     @Test
     public void testRunAsync(){
         for (int i = 0; i <1 ; i++) {
             try {
                 testServiceAsync.getRemoteRpc ();
             }catch (Exception e){
                 e.printStackTrace ();
             }
         }
     }

}
