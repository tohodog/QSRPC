package org.song.qsrpc.receiver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.ServerConfig;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年11月22日 下午2:04:37
 * <p>
 * 同一个tcp连接所有消息会由同一个线程进行处理
 */
public class TCPNodeHandler extends SimpleChannelInboundHandler<Message> {

    private static final Logger logger = LoggerFactory.getLogger(TCPNodeHandler.class);

    private MessageListener messageListener;
    private EventLoopGroup workerGroup;

    public TCPNodeHandler(EventLoopGroup workerGroup, MessageListener messageListener) {
        this.workerGroup = workerGroup;
        this.messageListener = messageListener;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Message msg) throws Exception {
        if (ServerConfig.VALUE_LOG)
            logger.info("receiverMessage-id:" + msg.getId() + ", channel:" + ctx.channel());

        Runnable work = new Runnable() {

            private boolean flag;

            @Override
            public void run() {
                try {
                    MessageListener.Async async = new MessageListener.Async() {
                        @Override
                        public void callBack(byte[] message) {
                            cb(message);
                        }
                    };
                    cb(messageListener.onMessage(async, msg.getContent()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            private void cb(byte[] message) {
                if (message == null) return;
                if (flag) throw new IllegalStateException("Has been called back!");

                Message msg_cb = new Message();
                msg_cb.setId(msg.getId());
                msg_cb.setZip(msg.getZip());
                msg_cb.setVer(msg.getVer());
                msg_cb.setContent(message);
                ctx.writeAndFlush(msg_cb);
                flag = true;
            }
        };

        // 1.如果rpc消息是同一个tcp过来的,所以都在同一个线程里处理,需要再分发出去. *但如果满足2,此操作可能会导致高并发延迟变大和上下文切换性能损耗
        // 2.发送端使用pool建立多个tcp链接可解决高并发延迟问题,又保证这边的性能不用再分发了,这个依赖客户端pool
        // 现改为配置设置,用户根据实际调试设置,一般不用改
        if (ServerConfig.VALUE_REDISTRIBUTE) {
            workerGroup.execute(work);
        } else {
            work.run();
        }
    }


    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("channelRegistered(" + ctx + ")");

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.info("channelActive(" + ctx + ")");

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.info("channelInactive(" + ctx + ")");

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        logger.info("channelUnregistered(" + ctx + ")");

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("exceptionCaught(" + ctx + ")", cause);

    }

}
