package thrift.xml.server.impl;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WmCreateAccountServiceImpl implements WmCreateAccountService.Iface {
    private AtomicInteger atomicInteger = new AtomicInteger ( 0 );
    @Override
    public WmCreateAccountRespone getRPC(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好" );
        if(new Random (  ).nextInt ( 5 )<10){
            throw new RuntimeException ( "测试错误" );
        }
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }
}
