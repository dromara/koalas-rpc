package xml.client;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import thrift.domain.*;
import thrift.service.WmCreateAccountService;

public class ThriftNative {
    public static final String SERVER_IP = "localhost";
    public static final int SERVER_PORT = 8001;
    public static final int TIMEOUT = 3000000;

    public static void main(String[] args) throws TException, KoalasRpcException, KoalasRpcException2, KoalasRpcException1 {
        TTransport transport = new TFramedTransport (new TSocket (SERVER_IP, SERVER_PORT, TIMEOUT));
        TProtocol protocol = new TBinaryProtocol (transport);
        WmCreateAccountService.Client client = new WmCreateAccountService.Client(protocol);
        transport.open();

        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是ThriftNative实现的服务端getRemoteRpc" );
        request.setPoiFlag ( 1 );

        WmCreateAccountRespone respone=client.getRPC (request  );
        System.out.println (respone);

    }
}
