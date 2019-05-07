package thrift.annotation.client.impl;

import annotation.KoalasClient;
import client.async.KoalasAsyncCallBack;
import generic.GenericRequest;
import generic.GenericService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service("testServiceAsync")
public class TestServiceAsync {
    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.AsyncIface wmCreateAccountService;

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000,genericService = "thrift.service.WmCreateAccountService")
    GenericService.AsyncIface genericService;

    public void getRemoteRpc() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.getRPC_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是注解实现的" );
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

    public void getGenericRemoteRpc() throws TException{
        KoalasAsyncCallBack<String, GenericService.AsyncClient.invoke_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();

        GenericRequest request = new GenericRequest (  );
        request.setMethodName ( "getRPC" );

        request.setClassType ( new ArrayList<String> (  ){{
            add ( "thrift.domain.WmCreateAccountRequest");
        }} );

        request.setRequestObj ( new ArrayList<String> (  ){{
            add ( "{\"accountType\":1,\"partnerId\":1,\"partnerName\":\"你好\",\"partnerType\":1,\"poiFlag\":1,\"setAccountType\":true,\"setPartnerId\":true,\"setPartnerName\":true,\"setPartnerType\":true,\"setPoiFlag\":true,\"setSource\":false,\"source\":0}");
        }} );

        genericService.invoke ( request,koalasAsyncCallBack );
        Future<String> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }


}
