package xml.client;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import thrift.service.WmCreateAccountService;
import thrift.xml.server.impl.WmCreateAccountServiceImpl;

public class ThriftNativeServer {
    public static final int SERVER_PORT = 8001;

    public static void main(String[] args1) throws TTransportException {

        TProcessor processor = new WmCreateAccountService.Processor<WmCreateAccountService.Iface> (new WmCreateAccountServiceImpl ());
        TNonblockingServerSocket tNonblockingServerSocket=new TNonblockingServerSocket ( SERVER_PORT );
        TThreadedSelectorServer.Args args = new  TThreadedSelectorServer.Args(tNonblockingServerSocket);
        args.processor ( processor );
        args.transportFactory ( new TFramedTransport.Factory() );
        args.protocolFactory ( new TBinaryProtocol.Factory (  ) );
        TThreadedSelectorServer server =new TThreadedSelectorServer(args);
        server.serve ();

    }
}
