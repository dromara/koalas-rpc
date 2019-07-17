package xml.client;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import thrift.domain.*;
import thrift.service.WmCreateAccountService;

public class ThriftNativeClient {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 8001;
    public static final int TIMEOUT = 3000000;

    public static void main(String[] args) throws TException, KoalasRpcException, KoalasRpcException2, KoalasRpcException1 {
       /* TTransport transport = new TFramedTransport (new TSocket (SERVER_IP, SERVER_PORT, TIMEOUT));
        TProtocol protocol = new TBinaryProtocol (transport);
        WmCreateAccountService.Client client = new WmCreateAccountService.Client(protocol);
        transport.open();

        long a =System.currentTimeMillis ();
        for (int i = 0; i < 100000; i++) {
            WmCreateAccountRequest request= new WmCreateAccountRequest (  );
            //request.setSource ( 10 );
            request.setAccountType ( 1 );
            request.setPartnerId ( 10 );
            request.setPartnerType ( 1 );
            request.setPartnerName ( "你好啊-我是ThriftNative实现的服务端getRemoteRpc" );
            request.setPoiFlag ( 1 );
            WmCreateAccountRespone respone=client.getRPC (request  );
        }
        long b =System.currentTimeMillis ();
        System.out.println (b-a);*/
        long a = System.currentTimeMillis ();
        Thread t1 =  new Thread ( new InnerRun () );
        Thread t2 =  new Thread ( new InnerRun () );
        Thread t3 =  new Thread ( new InnerRun () );
        Thread t4 =  new Thread ( new InnerRun () );
        Thread t5 =  new Thread ( new InnerRun () );
        Thread t6 =  new Thread ( new InnerRun () );
        Thread t7 =  new Thread ( new InnerRun () );
        Thread t8 =  new Thread ( new InnerRun () );
        Thread t9 =  new Thread ( new InnerRun () );
        Thread t10 = new Thread ( new InnerRun () );

        t1.start ();
        t2.start ();
        t3.start ();
        t4.start ();
        t5.start ();
        t6.start ();
        t7.start ();
        t8.start ();
        t9.start ();
        t10.start ();


        try {
            t1.join ();
            t2.join ();
            t3.join ();
            t4.join ();
            t5.join ();
            t6.join ();
            t7.join ();
            t8.join ();
            t9.join ();
            t10.join ();
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }

        System.out.println (System.currentTimeMillis ()-a);
    }

    private static class InnerRun implements Runnable{

        @Override
        public void run() {
            TTransport transport = new TFramedTransport (new TSocket (SERVER_IP, SERVER_PORT, TIMEOUT));
            TProtocol protocol = new TBinaryProtocol (transport);
            WmCreateAccountService.Client client = new WmCreateAccountService.Client(protocol);
            try {
                transport.open();
            } catch (TTransportException e) {
                e.printStackTrace ();
            }

            for (int i = 0; i < 100000; i++) {
                WmCreateAccountRequest request= new WmCreateAccountRequest (  );
                //request.setSource ( 10 );
                request.setAccountType ( 1 );
                request.setPartnerId ( 10 );
                request.setPartnerType ( 1 );
                request.setPartnerName ( "你好啊-我是ThriftNative实现的服务端getRemoteRpc" );
                request.setPoiFlag ( 1 );
                try {
                    WmCreateAccountRespone respone=client.getRPC (request  );
                } catch (Exception e) {
                    e.printStackTrace ();
                }
            }
            long b =System.currentTimeMillis ();
        }
    }

}
