package org.song.qsrpc.send;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.cb.CallbackPool;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年11月22日 下午2:04:37
 * <p>
 * tcp连接类nio回调类
 */
public class TCPRouteHandler extends SimpleChannelInboundHandler<Message> {

    private static final Logger logger = LoggerFactory.getLogger(TCPRouteHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (ServerConfig.RPC_CONFIG.isPrintLog()) {
            logger.debug("callMessage-id:" + msg.getId() + ", channel:" + ctx.channel());
        }
        @SuppressWarnings("unchecked")
        Callback<Message> cb = (Callback<Message>) CallbackPool.remove(msg.getId());
        if (cb == null) {
            //找不到回调//可能超时被清理了
            logger.warn("Receive msg from server but not context found, requestId=" + msg.getId() + "," + ctx);
            return;
        }
        cb.handleResult(msg);
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
