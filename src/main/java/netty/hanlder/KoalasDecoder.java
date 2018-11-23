package netty.hanlder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.KoalasServerPublisher;

import java.util.List;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class KoalasDecoder extends ByteToMessageDecoder {

    private final static Logger logger = LoggerFactory.getLogger ( KoalasDecoder.class );

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        try {
            if (in.readableBytes () < 4) {
                return;
            }

            in.markReaderIndex ();
            byte[] b = new byte[4];
            in.readBytes ( b );

            int length = decodeFrameSize ( b );

            if (in.readableBytes () < length) {
                //reset the readerIndex
                in.resetReaderIndex ();
                return;
            }

            in.resetReaderIndex ();
            ByteBuf fream = in.readRetainedSlice ( 4 + length );
            in.resetReaderIndex ();

            in.skipBytes ( 4 + length );
            out.add ( fream );
        } catch (Exception e) {
            logger.error ( "decode error",e );
        }

    }

    public static final int decodeFrameSize(byte[] buf) {
        return (buf[0] & 255) << 24 | (buf[1] & 255) << 16 | (buf[2] & 255) << 8 | buf[3] & 255;
    }
}
