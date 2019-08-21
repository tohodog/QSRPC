package org.song.qsrpc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 *
 * @author song
 *
 * @Email vipqinsong@gmail.com
 *
 * @date 2018年11月22日 上午11:55:38
 *
 *       适用短连接/追求效率+比较可靠的长连接,包头长度+特定包尾,一旦丢包必须关闭重连,因为后面数据会错乱
 *       <p>
 *       长度(4) 包id(4)类型(1)预留字节(5)内容(n)包尾(2)
 */
public class MessageDecoder extends ByteToMessageDecoder {

	private static final Logger logger = LoggerFactory.getLogger(MessageDecoder.class);

	public static final int MAX_LEN = 1024 * 1024 * 64;

	private Message message;
	private int msgLength;

	@Override
	public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

//		System.out.println(String.format("capacity:%s readerIndex:%s writeIndex:%s readeableBytes:%s writableBytes:%s",
//				in.capacity(), in.readerIndex(), in.writerIndex(), in.readableBytes(), in.writableBytes()));

		if (message == null && in.readableBytes() < 8) {
			return;
		}

		if (message == null) {
			msgLength = in.readInt();
			if (msgLength < 12 || msgLength > MAX_LEN) {// 传输出错了,错位
				logger.error("decode-len_err(" + ctx + ")-len:" + msgLength);
				init();
				ctx.close();
				return;
			}
			message = new Message();
			// in.markReaderIndex();
		}

		// 判断已接收内容长度
		if (in.readableBytes() < msgLength) {
			// in.resetReaderIndex();
			return;
		}
		// 内容足够了,开始读取
		message.setId(in.readInt());
		message.setType(in.readByte());
		in.readerIndex(in.readerIndex() + 5);
		byte[] content = new byte[msgLength - 12];
		in.readBytes(content);
		message.setContent(content);

		if (in.readByte() != '\r' || in.readByte() != '\n') {
			logger.error("decode-end_err(" + ctx + ")");
			init();
			ctx.close();
			return;
		}

		out.add(message);
		init();
	}

	private void init() {
		message = null;
		msgLength = 0;
	}

}
