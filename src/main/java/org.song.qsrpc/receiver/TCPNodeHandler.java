package org.song.qsrpc.receiver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import org.song.qsrpc.Message;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年11月22日 下午2:04:37
 * <p>
 * 同一个tcp连接所有消息会由同一个线程进行处理
 */
public class TCPNodeHandler extends SimpleChannelInboundHandler<Message> {

    private MessageListener messageListener;
    private EventLoopGroup workerGroup;

    public TCPNodeHandler(EventLoopGroup workerGroup, MessageListener messageListener) {
        this.workerGroup = workerGroup;
        this.messageListener = messageListener;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Message msg) throws Exception {

        System.out.println(
                "TCPNodeHandler-HandlerMessage" + msg.getId() + "-" + Thread.currentThread().getName());
        // rpc消息基本都是同一个tcp过来的,所以都在同一个线程里处理,需要再分发出去
        workerGroup.execute(new Runnable() {
            @Override
            public void run() {
                Message msg_cb = new Message();
                msg_cb.setId(msg.getId());
                msg_cb.setContent(messageListener.onMessage(msg.getContent()));
                ctx.writeAndFlush(msg_cb);
            }
        });
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("TCPNodeHandler-channelRegistered(" + ctx + ")");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("TCPNodeHandler-channelActive(" + ctx + ")");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("TCPNodeHandler-channelInactive(" + ctx + ")");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        System.out.println("TCPNodeHandler-channelUnregistered(" + ctx + ")");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        System.out.println("TCPNodeHandler-exceptionCaught(" + ctx + ")\n" + cause);
    }

}
