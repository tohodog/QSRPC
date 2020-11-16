package org.song.qsrpc.receiver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.send.RPCClientManager;

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
//        logger.info("receiverMessage-id:" + msg.getId() + ", channel:" + ctx.channel());

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
                msg_cb.setContent(message);
                ctx.writeAndFlush(msg_cb);
                flag = true;
            }
        };

        work.run();
        // rpc消息基本都是同一个tcp过来的,所以都在同一个线程里处理,需要再分发出去
        //此操作会导致延迟变大!!,禁用
//        workerGroup.execute(work);
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
