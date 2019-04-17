package thrift.annotation.client.impl;


import annotation.KoalasClient;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

@Service("testServiceSync")
public class TestServiceSync {

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.Iface wmCreateAccountService;

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.Iface wmCreateAccountService1;

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

}
