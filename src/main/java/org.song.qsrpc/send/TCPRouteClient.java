package org.song.qsrpc.send;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.song.qsrpc.Message;
import org.song.qsrpc.MessageDecoder;
import org.song.qsrpc.MessageEncoder;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.send.cb.CallFuture;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.cb.CallbackPool;

import java.util.concurrent.TimeUnit;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年2月25日 下午5:22:31
 * <p>
 * tcp路由连接客户端
 */
public class TCPRouteClient {

    // 连接配置,需要再独立成配置类
    private static final int connTimeout = 18 * 1000;

    private static final boolean soKeepalive = true;

    private static final boolean soReuseaddr = true;

    private static final boolean tcpNodelay = true;

    private static final int soRcvbuf = 1024 * 128;

    private static final int soSndbuf = 1024 * 128;

    private String ip;
    private int port;

    private SslContext sslContext;

    private Channel channel;
    private EventLoopGroup bossGroup;// TODO 考虑改成静态,所有连接公用同一个线程池
    // private static EventLoopGroup nioGroup = new NioEventLoopGroup();

    // private CountDownLatch countDownLatch;

    // Lock lock=new ReentrantLock();

    public TCPRouteClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void setSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
    }

    public void connect() {
        if (isConnect())
            return;
        try {
            bossGroup = new NioEventLoopGroup(1);//
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connTimeout);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, soKeepalive);
            bootstrap.option(ChannelOption.SO_REUSEADDR, soReuseaddr);
            bootstrap.option(ChannelOption.TCP_NODELAY, tcpNodelay);
            bootstrap.option(ChannelOption.SO_RCVBUF, soRcvbuf);
            bootstrap.option(ChannelOption.SO_SNDBUF, soSndbuf);

            bootstrap.group(bossGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ChannelPipeline pipeline = ch.pipeline();
                    if (sslContext != null) {
                        pipeline.addLast(sslContext.newHandler(ch.alloc()));
                    }

                    pipeline.addLast(new MessageEncoder());// tcp消息编码
                    pipeline.addLast(new MessageDecoder());// tcp消息解码
                    pipeline.addLast(new TCPRouteHandler());

                    // 以"$_"作为分隔符
                    /*
                     * ChannelPipeline pipeline = ch.pipeline(); pipeline.addLast("encoder", new
                     * StringEncoder(CharsetUtil.UTF_8)); String s = "$_"; ByteBuf byteBuf =
                     * Unpooled.copiedBuffer(s.getBytes()); pipeline.addLast(new
                     * DelimiterBasedFrameDecoder(Integer.MAX_VALUE,byteBuf)); pipeline.addLast(new
                     * StringDecoder()); pipeline.addLast(new MyHeartSocket());
                     */

                }
            });
            // 发起连接操作
            ChannelFuture channelFuture = bootstrap.connect(ip, port).awaitUninterruptibly();// .sync();
            channel = channelFuture.channel();

            channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {

                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {

                }
            });

            // 等待监听端口关闭
            // channel.closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
        }
    }

    public void close() {
        if (channel != null)
            channel.close();
        if (bossGroup != null)
            bossGroup.shutdownGracefully();

    }

    public boolean isConnect() {
        return (channel != null && channel.isOpen() && channel.isActive());
    }

    public String getInfo() {
        if (channel != null)
            return channel.toString();
        else
            return ip + ":" + port;
    }

    /**
     * 异步发送,nio
     */
    public void sendAsync(Message request, Callback<Message> callback, int timeout) {
        if (isConnect()) {
            CallbackPool.put(String.valueOf(request.getId()), callback, timeout);
            channel.writeAndFlush(request);
        } else {
            callback.handleError(new RPCException(this.getClass().getName() + "-can no connect:" + getInfo()));
        }

    }

    /**
     * 同步,返回响应信息 RPC不建议用,访问延迟大将会导致线程挂起太久,CPU无法跑满,而解决方法只有新建更多线程,性能不好
     */
    public Message sendSync(Message request, int timeout) throws InterruptedException, RPCException {
        if (isConnect()) {
            CallFuture<Message> future = CallFuture.newInstance();
            CallbackPool.put(String.valueOf(request.getId()), future);
            channel.writeAndFlush(request);
            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } finally {
                CallbackPool.remove(String.valueOf(request.getId()));
            }
        } else {
            throw new RPCException("TCPClient.sendSync() can no connect:" + getInfo());
        }
    }

}
