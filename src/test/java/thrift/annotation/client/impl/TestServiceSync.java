package thrift.annotation.client.impl;


import annotation.KoalasClient;
import generic.GenericRequest;
import generic.GenericService;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.ArrayList;

@Service("testServiceSync")
public class TestServiceSync {

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.Iface wmCreateAccountService;

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000,genericService = "thrift.service.WmCreateAccountService")
    GenericService.Iface genericService;

    public void getRemoteRpc() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是注解实现的" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.getRPC (  request);
        System.out.println (respone);
     }

    public void getGenericRemoteRpc() throws TException {
        GenericRequest request = new GenericRequest (  );
        request.setMethodName ( "getRPC" );

        request.setClassType ( new ArrayList<String> (  ){{
            add ( "thrift.domain.WmCreateAccountRequest");
        }} );

        request.setRequestObj ( new ArrayList<String> (  ){{
            add ( "{\"accountType\":1,\"partnerId\":1,\"partnerName\":\"你好\",\"partnerType\":1,\"poiFlag\":1,\"setAccountType\":true,\"setPartnerId\":true,\"setPartnerName\":true,\"setPartnerType\":true,\"setPoiFlag\":true,\"setSource\":false,\"source\":0}");
        }} );

        String str = genericService.invoke ( request );
        System.out.println (str);
    }

}
