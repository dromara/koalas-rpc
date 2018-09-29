package netty.hanlder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;

public class KoalasEncoder extends MessageToByteEncoder<ByteArrayOutputStream> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteArrayOutputStream msg, ByteBuf out) throws Exception {
        out.writeBytes ( msg.toByteArray () );
    }
}
