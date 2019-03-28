package thrift.xml.client.impl;

import client.async.KoalasAsyncCallBack;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service("testServiceAsync")
public class TestServiceAsync {
    @Autowired
    WmCreateAccountService.AsyncIface wmCreateAccountService;
    public void getRemoteRpc() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.getRPC_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.getRPC ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }


    public void koaloasTest1() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest1_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest1" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest1 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

    public void koaloasTest2() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest2_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest2" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest2 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

    public void koaloasTest3() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest3_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest3" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest3 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

    public void koaloasTest4() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest4_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest3" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest4 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }


    public void koaloasTest5() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest5_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest3" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest5 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }


    public void koaloasTest6() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.koaloasTest6_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊koaloasTest3" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.koaloasTest6 ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

}
