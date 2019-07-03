package thrift.xml.client.impl;


import com.alibaba.fastjson.JSON;
import generic.GenericRequest;
import generic.GenericService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import thrift.domain.*;
import thrift.service.WmCreateAccountService;

import java.util.ArrayList;

@Service("testServiceSync")
public class TestServiceSync {

    @Autowired
    WmCreateAccountService.Iface wmCreateAccountService;

    @Autowired
    @Qualifier("wmCreateAccountService3")
    GenericService.Iface wmGenericService;

    public void getRemoteRpc() throws TException, KoalasRpcException, KoalasRpcException2, KoalasRpcException1 {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId (9 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端getRemoteRpc-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.getRPC (  request);
        System.out.println (respone);
     }

    public void koaloasTest1() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest1-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest1 (  request);
        System.out.println (respone);
    }

    public void koaloasTest2() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest2-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest2 (  request);
        System.out.println (respone);
    }

    public void koaloasTest3() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest3-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest3 (  request);
        System.out.println (respone);
    }

    public void koaloasTest4() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest4-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest4 (  request);
        System.out.println (respone);
    }

    public void koaloasTest5() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest5-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest5 (  request);
        System.out.println (respone);
    }

    public void koaloasTest6() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是xml实现的服务端koaloasTest6-异步" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest6 (  request);
        System.out.println (respone);
    }

    public void getGenericRpc() throws TException {
        GenericRequest request = new GenericRequest (  );
        request.setMethodName ( "getRPC" );

        request.setClassType ( new ArrayList<String> (  ){{
            add ( "thrift.domain.WmCreateAccountRequest");
        }} );

        request.setRequestObj ( new ArrayList<String> (  ){{
            add ( "{\"accountType\":1,\"partnerId\":1,\"partnerName\":\"你好\",\"partnerType\":1,\"poiFlag\":1,\"source\":0}");
        }} );

        String str = wmGenericService.invoke ( request );
        System.out.println (str);
    }

    public static void main(String[] args) {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好" );
        request.setPoiFlag ( 1 );
        System.out.println (JSON.toJSONString (request));;
    }

}
