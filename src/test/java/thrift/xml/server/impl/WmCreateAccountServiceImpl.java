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
        if(new Random (  ).nextInt ( 5 )>0){
            throw new RuntimeException ( "测试错误" );
        }
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest1(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest1" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest2(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest2" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest3(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest3" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest4(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest4" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest5(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest5" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest6(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest6" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest7(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest7" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest8(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest8" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest9(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest9" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest10(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest10" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest11(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest11" );
        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest12(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好koaloasTest12" );
        return wmCreateAccountRespone;
    }
}
