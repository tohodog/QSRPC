package org.song.qsrpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 *
 * @author song
 *
 * @Email vipqinsong@gmail.com
 *
 * @date 2018年11月22日 上午11:55:38
 *
 *       适用短连接/追求效率+比较可靠的长连接 包头长度+特定包尾 一旦丢包必须关闭重连,因为后面数据会错乱
 *       <p>
 *       长度(4) 包id(4)类型(1)预留字节(5)内容(n)包尾(2)
 *
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

	public final static byte[] BYTE_END = new byte[] { '\r', '\n' };

	@Override
	public void encode(ChannelHandlerContext ctx, Message in, ByteBuf out) throws Exception {
		byte[] content = in.getContent();
		if (content != null) {
			out.writeInt(content.length + 12);
			out.writeInt(in.getId());
			out.writeByte(in.getType());
			out.writerIndex(out.writerIndex() + 5);
			out.writeBytes(content);
			out.writeBytes(BYTE_END);
		}
	}

}
