package thrift.xml.client.impl;


import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

@Service("testServiceSync")
public class TestServiceSync {

    @Autowired
    WmCreateAccountService.Iface wmCreateAccountService;

    public void getRemoteRpc() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
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
        request.setPartnerName ( "你好" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.koaloasTest6 (  request);
        System.out.println (respone);
    }

}
