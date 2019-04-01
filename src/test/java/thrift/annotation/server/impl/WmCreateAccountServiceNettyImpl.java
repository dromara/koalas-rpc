package thrift.annotation.server.impl;

import annotation.KoalasServer;
import org.apache.thrift.TException;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@KoalasServer ( port = 8801,zkpath="127.0.0.1:2181")
public class WmCreateAccountServiceNettyImpl implements WmCreateAccountService.Iface {
    private AtomicInteger atomicInteger = new AtomicInteger ( 0 );
    @Override
    public WmCreateAccountRespone getRPC(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好啊" );
        if(new Random (  ).nextInt ( 5 )>100){
            try {
                Thread.sleep ( 5000 );
            } catch (InterruptedException e) {
                e.printStackTrace ();
            }
        }
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest1(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest1" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest2(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest2" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest3(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest3" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest4(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest4" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest5(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest5" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest6(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest6" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest7(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest7" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest8(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest8" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest9(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest9" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest10(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest10" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest11(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest11" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }

    @Override
    public WmCreateAccountRespone koaloasTest12(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "koalasTest12" );
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }
}
