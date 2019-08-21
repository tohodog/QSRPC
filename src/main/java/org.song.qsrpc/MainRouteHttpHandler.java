//package org.song.qsrpc;
//
//import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//
//import org.song.qserver.framework.Constants;
//import org.song.qserver.framework.Log;
//import org.song.qserver.framework.Request;
//import org.song.qserver.framework.Response;
//import org.song.qserver.framework.rpc.send.RPCClientManager;
//import org.song.qserver.framework.rpc.send.cb.Callback;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.ChannelFuture;
//import io.netty.channel.ChannelFutureListener;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelProgressiveFuture;
//import io.netty.channel.ChannelProgressiveFutureListener;
//import io.netty.channel.DefaultFileRegion;
//import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.handler.codec.http.DefaultFullHttpResponse;
//import io.netty.handler.codec.http.DefaultHttpResponse;
//import io.netty.handler.codec.http.FullHttpResponse;
//import io.netty.handler.codec.http.HttpChunkedInput;
//import io.netty.handler.codec.http.HttpContent;
//import io.netty.handler.codec.http.HttpHeaderValues;
//import io.netty.handler.codec.http.HttpObject;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpResponse;
//import io.netty.handler.codec.http.HttpResponseStatus;
//import io.netty.handler.codec.http.HttpVersion;
//import io.netty.handler.codec.http.LastHttpContent;
//import io.netty.handler.ssl.SslHandler;
//import io.netty.handler.stream.ChunkedFile;
//import io.netty.util.CharsetUtil;
//
///**
// *
// * @author song
// *
// * @Email vipqinsong@gmail.com
// *
// * @date 2019年5月10日
// *
// *       接收外部http请求,主路由分发处理
// */
//public class MainRouteHttpHandler extends SimpleChannelInboundHandler<HttpObject> {
//
////	private HttpDataFactory httpDataFactory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
//
//	private Request request;
////	private HttpPostRequestDecoder contentDecoder;
//	private ByteBuf byteBuf;
//	private boolean isKeepAlive;// 开了会复用管道对象,提升tls,目前默认关闭,基本客户端都会断开
//	@SuppressWarnings("unused")
//	private int bodyLen;
//
//	private void init() {
////		if (contentDecoder != null)
////			contentDecoder.destroy();
////		contentDecoder = null;
//
//		if (byteBuf != null)
//			byteBuf.release();
//		byteBuf = null;
//		request = null;
//		bodyLen = 0;
//	}
//
//	@Override
//	public void channelRead0(ChannelHandlerContext context, HttpObject msg) {
//
//		if (msg instanceof HttpRequest) {
//			init();
//			request = new Request(context, (HttpRequest) msg);
//			if (!request.isSuccess()) {
//				sendError(context, HttpResponseStatus.BAD_REQUEST);
//				return;
//			}
//			// isKeepAlive = request.isKeepAlive();
//
//			switch (request.method()) {
//			case "POST":
//			case "PUT":
//			case "PATCH":
//				// http内容解析器
////				String contentType = request.contentType();
////				if (contentType != null && (contentType.contains("application/x-www-form-urlencoded")||
////						 contentType.contains("multipart/form-data")))
////					contentDecoder = new HttpPostRequestDecoder(httpDataFactory, (HttpRequest) msg, request.charset());// 支持其他编码
//				// multipart上传的内容不写入容器了,直接解析了不浪费内存
////				if (contentType == null || !contentType.contains("multipart/form-data"))
//				byteBuf = Unpooled.buffer(0);// http请求内容体容器
//			}
//		}
//
//		if (msg instanceof HttpContent && request != null) {
//			try {
//				bodyLen += ((HttpContent) msg).content().readableBytes();
////				if (contentDecoder != null) {
////					contentDecoder.offer((HttpContent) msg);
////				}
//				if (byteBuf != null) {
//					// 重置读取index
//					((HttpContent) msg).content().resetReaderIndex();
//					// 其他post请求数据,@TODO 优化: 如果大于一个阈值,写入文件
//					byteBuf.writeBytes(((HttpContent) msg).content());
//				}
//			} catch (Exception e) {
//				// 服务器处理出错
//				sendError(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
//				return;
//			}
//			// 请求未完成
//			if (!(msg instanceof LastHttpContent))
//				return;
//
////			request.contentDecoder(contentDecoder);
//			request.contentByteBuf(byteBuf);
//			handleBusiness(context);
//		}
//	}
//
//	// 处理接口业务
//	private void handleBusiness(final ChannelHandlerContext context) {
//		try {
//			if (request.path() == null || request.path().length() < 1) {
//				sendError(context, HttpResponseStatus.NOT_FOUND);
//			}
//
//			RPCClientManager.getInstance().sendAsync(request.path().split("/")[1],
//					DataConversion.Request2Message(request), new Callback<Message>() {
//
//						@Override
//						public void handleResult(Message result) {
//							try {
//								Response response = DataConversion.Message2Response(result);
//								Object contentBody = response.contentBody();
//								if (contentBody instanceof String || contentBody instanceof byte[]
//										|| contentBody == null)
//									sendResult(context, response);
//								else
//									sendFile(context, response);
//							} catch (IOException e) {
//								e.printStackTrace();
//								sendError(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
//							}
//						}
//
//						@Override
//						public void handleError(Throwable e) {
//							sendError(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
//						}
//					});
//		} catch (Exception e) {
//			// RPC服务器处理出错
//			sendError(context, HttpResponseStatus.INTERNAL_SERVER_ERROR, e);
//		} finally {
//		}
//	}
//
//	// 返回响应内容
//	private void sendResult(ChannelHandlerContext context, Response responce) {
//
//		ByteBuf byteBuf = null;
//		Object contentBody = responce.contentBody();
//		if (contentBody instanceof String)
//			byteBuf = Unpooled.wrappedBuffer(((String) contentBody).getBytes(responce.charset()));
//		else if (contentBody instanceof byte[])
//			byteBuf = Unpooled.wrappedBuffer((byte[]) contentBody);
//		else
//			byteBuf = Unpooled.buffer(0);
//
//		FullHttpResponse msg = new DefaultFullHttpResponse(HTTP_1_1, responce.status(), byteBuf);
//
//		if (responce.headers() != null) {
//			for (CharSequence key : responce.headers().keySet())
//				msg.headers().set(key, responce.headers().get(key));
//		}
//		msg.headers().set(Constants.HEAD_CL, msg.content().readableBytes());
//
//		if (!isKeepAlive) {
//			context.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
//		} else {
//			msg.headers().set(Constants.HEAD_CONN, HttpHeaderValues.KEEP_ALIVE);
//			context.writeAndFlush(msg);
//		}
//		init();
//	}
//
//	// 返回响应文件
//	private void sendFile(ChannelHandlerContext context, Response responce) throws IOException {
//		// 返回消息头构建
//		HttpResponse msg = new DefaultHttpResponse(HTTP_1_1, responce.status());
//
//		if (isKeepAlive) {
//			msg.headers().set(Constants.HEAD_CONN, HttpHeaderValues.KEEP_ALIVE);
//		}
//		if (responce.headers() != null) {
//			for (CharSequence key : responce.headers().keySet())
//				msg.headers().set(key, responce.headers().get(key));
//		}
//
//		// 返回文件
//		Object contentBody = responce.contentBody();
//		RandomAccessFile raf = null;
//		if (contentBody instanceof RandomAccessFile)
//			raf = (RandomAccessFile) contentBody;
//		else if (contentBody instanceof File)
//			raf = new RandomAccessFile((File) contentBody, "r");
//
//		ChannelFuture lastContentFuture;
//
//		if (raf == null) {
//			msg.headers().set(Constants.HEAD_CL, 0);
//			context.write(msg);
//			lastContentFuture = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//		} else {
//			long fileLength = raf.length();
//			long filePointer = raf.getFilePointer();
//			msg.headers().set(Constants.HEAD_CL, fileLength - filePointer);
//			// 响应头写入
//			context.write(msg);
//			// 文件写入
//			ChannelFuture sendFileFuture = null;
//			if (context.pipeline().get(SslHandler.class) == null) {
//				sendFileFuture = context.write(
//						new DefaultFileRegion(raf.getChannel(), filePointer, fileLength - filePointer),
//						context.newProgressivePromise());
//				// 结束标记
//				lastContentFuture = context.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//			} else {
//				sendFileFuture = context.writeAndFlush(
//						new HttpChunkedInput(new ChunkedFile(raf, filePointer, fileLength - filePointer, 8192)),
//						context.newProgressivePromise());
//				// HttpChunkedInput will write the end marker (LastHttpContent) for us.
//				lastContentFuture = sendFileFuture;
//			}
//			// 文件进度监听
//			sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
//				@Override
//				public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
//					if (total < 0) { // total unknown
//						System.out.println(future.channel() + " Transfer progress: " + progress);
//					} else {
//						System.out.println(future.channel() + " Transfer progress: " + progress + " / " + total);
//					}
//				}
//
//				@Override
//				public void operationComplete(ChannelProgressiveFuture future) {
//					System.out.println(future.channel() + " Transfer complete.");
//				}
//			});
//		}
//
//		if (!isKeepAlive) {
//			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
//		}
//		init();
//	}
//
//	// 返回错误码
//	private void sendError(ChannelHandlerContext context, HttpResponseStatus badRequest) {
//		sendError(context, badRequest, null);
//	}
//
//	// 返回错误码
//	private void sendError(ChannelHandlerContext context, HttpResponseStatus badRequest, Throwable e) {
//		if (e == null) {
//			Log.i(getClass().getName() + "\n" + request.toString() + "\nResponse: " + badRequest.toString() + "\n");
//		} else {
//			Log.e(getClass().getName() + "\n" + request.toString() + "\nResponse: " + badRequest.toString(), e);
//		}
//		ByteBuf errorInfo = Unpooled.copiedBuffer("Failure:" + badRequest + " o(╥﹏╥)o\r\n", CharsetUtil.UTF_8);
//		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, badRequest, errorInfo);
//		response.headers().set(Constants.HEAD_CT, Constants.CT_TEXT);
//		response.headers().set(Constants.HEAD_CL, response.content().readableBytes());
//		context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//		init();
//	}
//
//	@Override
//	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//		super.channelRegistered(ctx);
//		// System.out.println("channelRegistered(" + ctx + ")");
//	}
//
//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		super.channelActive(ctx);
//		// System.out.println("channelActive(" + ctx + ")");
//
//	}
//
//	@Override
//	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//		super.channelInactive(ctx);
//		// System.out.println("channelInactive(" + ctx + ")");
//
//	}
//
//	@Override
//	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//		super.channelRegistered(ctx);
//		// System.out.println("channelUnregistered(" + ctx + ")");
//
//	}
//
//	@Override
//	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//		// super.exceptionCaught(ctx, cause);
//		System.out.println(getClass().getName() + ".exceptionCaught(" + ctx + ")\n" + cause);
//		// Log.e(getClass().getName() + "\n" + ctx, cause);
//	}
//
//}
