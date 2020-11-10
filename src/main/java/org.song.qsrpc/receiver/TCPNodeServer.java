package org.song.qsrpc.receiver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.song.qsrpc.Log;
import org.song.qsrpc.MessageDecoder;
import org.song.qsrpc.MessageEncoder;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.zk.NodeInfo;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

public class TCPNodeServer {

    private final int PORT;
    private NodeInfo nodeInfo;
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private Channel channel;
    private MessageListener messageListener;

    public TCPNodeServer(NodeInfo nodeInfo, MessageListener messageListener) {
        this.nodeInfo = nodeInfo;
        this.messageListener = messageListener;
        PORT = nodeInfo.getPort();
    }

    public boolean start() {
        if (isConnect()) return true;

        // Configure SSL.
        final SslContext sslCtx = null;// getSslContext();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();// 默认cpu线程*2
        try {
            ServerBootstrap b = new ServerBootstrap();
            // BACKLOG用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度。如果未设置或所设置的值小于1，Java将使用默认值50
            b.option(ChannelOption.SO_BACKLOG, 1024 * 8);
            // 是否启用心跳保活机制。在双方TCP套接字建立连接后（即都进入ESTABLISHED状态）如果在两小时内没有数据的通信时，TCP会自动发送一个活动探测数据报文
            b.option(ChannelOption.SO_KEEPALIVE, true);
            // 用于启用或关闭Nagle算法。如果要求高实时性，有数据发送时就马上发送，就将该选项设置为true关闭Nagle算法；如果要减少发送次数减少网络交互，就设置为false等累积一定大小后再发送。默认为false。
            b.option(ChannelOption.TCP_NODELAY, false);
            // 缓冲区大小
            // b.option(ChannelOption.SO_RCVBUF, 128 * 1024);
            // b.option(ChannelOption.SO_SNDBUF, 128 * 1024);

            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    // .handler(new LoggingHandler(LogLevel.INFO)) //日记
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {

                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslCtx != null) {
                                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            //pipeline.addLast(new LengthFieldBasedFrameDecoder(1024*1024, 0, 4, 0, 0));//组合消息包,参数0是消息最大长度,1,2参数是长度字段的位置,3是长度调整量,4去掉包头
                            pipeline.addLast(new MessageEncoder());// tcp消息编码
                            pipeline.addLast(new MessageDecoder());//  tcp消息解码

                            pipeline.addLast(new TCPNodeHandler(workerGroup, messageListener));
                        }
                    });

            channel = b.bind(PORT).sync().channel();//tcp监听完成

            Log.i("NodeServer Launcher Success! ^_^ PORT:" + PORT);
            closeFuture();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            close();
            Log.e("NodeServer Launcher Fail T^T: " + e.getMessage());
            return false;
        }
    }

    private void closeFuture() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.closeFuture().sync();
                    close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void close() {
        if (isConnect()) channel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        channel = null;
        bossGroup = null;
        workerGroup = null;
        Log.i("NodeServer Close Success! -^- PORT:" + PORT);
    }

    public boolean isConnect() {
        return (channel != null && channel.isOpen() && channel.isActive());
    }


    public static SslContext getSslContext() throws Exception {
        if (!ServerConfig.containsKey(ServerConfig.KEY_SSL_JKS_PATH)) {
            return null;
        }
        File jks = new File(ServerConfig.getString(ServerConfig.KEY_SSL_JKS_PATH));

        // 客户端证书,也可以放在jks里
        File cert = null;
        if (ServerConfig.containsKey(ServerConfig.KEY_SSL_CERT_PATH)) {
            cert = new File(ServerConfig.getString(ServerConfig.KEY_SSL_CERT_PATH));
        }

        String keystorePassword = null;
        if (ServerConfig.containsKey(ServerConfig.KEY_SSL_JKS_PASSWORD))
            keystorePassword = ServerConfig.getString(ServerConfig.KEY_SSL_JKS_PASSWORD);

        return SslContextBuilder.forServer(cert, jks, keystorePassword).build();
    }

    public static SSLContext getSSLContext() throws Exception {
        if (!ServerConfig.containsKey(ServerConfig.KEY_SSL_JKS_PATH)) {
            return null;
        }

        char[] keystorePassword = null;
        if (ServerConfig.containsKey(ServerConfig.KEY_SSL_JKS_PASSWORD))
            keystorePassword = ServerConfig.getString(ServerConfig.KEY_SSL_JKS_PASSWORD).toCharArray();

        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(ServerConfig.getString(ServerConfig.KEY_SSL_JKS_PASSWORD)), keystorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, keystorePassword);

        SSLContext serverContext = SSLContext.getInstance("SSLv3");
        serverContext.init(kmf.getKeyManagers(), null, null);

        return serverContext;
    }

}
