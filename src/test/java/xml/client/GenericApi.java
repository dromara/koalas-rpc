package xml.client;

import client.proxyfactory.KoalasClientProxy;
import generic.GenericRequest;
import generic.GenericService;
import org.apache.thrift.TException;

import java.util.ArrayList;

public class GenericApi {

    public static void main(String[] args) throws TException {

        KoalasClientProxy koalasClientProxy = new KoalasClientProxy();
        koalasClientProxy.setServiceInterface ( "thrift.service.WmCreateAccountService" );
        koalasClientProxy.setZkPath ("127.0.0.1:2181"  );
        koalasClientProxy.setGeneric ( true );
        koalasClientProxy.setReadTimeout ( 50000000 );
        koalasClientProxy.afterPropertiesSet ();
        GenericService.Iface genericService = (GenericService.Iface) koalasClientProxy.getObject ();
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
        koalasClientProxy.destroy ();
    }

}
