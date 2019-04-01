package xml.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:xml/server/koalas-server-netty.xml"})
public class ServerRunNetty {

    @Test
    public void testServerRun() throws InterruptedException {
         Thread.sleep ( 10000000 );
    }
}
