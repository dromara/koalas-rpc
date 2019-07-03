package thrift.xml.server.impl;

import org.apache.thrift.TException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;
import thrift.domain.*;
import thrift.service.WmCreateAccountService;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WmCreateAccountServiceImpl implements WmCreateAccountService.Iface {
    private AtomicInteger atomicInteger = new AtomicInteger ( 0 );


    @Override
    public WmCreateAccountRespone getRPC(WmCreateAccountRequest wmCreateAccountRequest) throws  KoalasRpcException, KoalasRpcException1, KoalasRpcException2, TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好" );
        if(wmCreateAccountRequest.getPartnerId ()==1){
            throw  new KoalasRpcException(1,"123");
        }
        if(wmCreateAccountRequest.getPartnerId ()==2){
            throw  new KoalasRpcException1(2,"456");
        }
        if(wmCreateAccountRequest.getPartnerId ()==3){
            throw  new KoalasRpcException2(3,"789");
        }
        if(wmCreateAccountRequest.getPartnerId ()==4){
            throw  new UncategorizedSQLException ("123","select * from xxx1",new SQLException ("456"));
        }
        if(wmCreateAccountRequest.getPartnerId ()==5){
            throw  new UncategorizedSQLException ("123","select * from xxx2",new SQLException ("456"));
        }
        if(wmCreateAccountRequest.getPartnerId ()==6){
            throw  new TException ("123",new Exception ("456"));
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
