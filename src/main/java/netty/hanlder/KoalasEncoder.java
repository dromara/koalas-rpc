package netty.hanlder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalasEncoder extends MessageToByteEncoder<ByteArrayOutputStream> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteArrayOutputStream msg, ByteBuf out) throws Exception {
        out.writeBytes ( msg.toByteArray () );
    }
}
