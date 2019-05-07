package annotation.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import thrift.annotation.client.impl.TestServiceAsync;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:annotation/client/koalas-client.xml"})
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

    @Test
    public void testRunGenericAsync(){
        for (int i = 0; i <100 ; i++) {
            try {
                testServiceAsync.getGenericRemoteRpc ();
            }catch (Exception e){
                e.printStackTrace ();
            }
        }
    }

}
