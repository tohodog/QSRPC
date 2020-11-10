package org.song.qsrpc.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.send.cb.CallFuture;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.pool.ClientPool;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 管理操作类,负责管理操作各个模块
 */
public class RPCClientManager {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientManager.class);

    public static final int RpcTimeout;

    static {
        if (ServerConfig.containsKey(ServerConfig.KEY_RPC_CONNECT_TIMEOUT)) {
            RpcTimeout = ServerConfig.getInt(ServerConfig.KEY_RPC_CONNECT_TIMEOUT);
        } else {
            RpcTimeout = 60 * 1000;
        }
    }

    /**
     * true:
     * 拿出连接对象发送信息后,马上放回pool,nio设计,连接池只要几个链接就够了(因为TCPClient支持全双工,所以可以同时发消息,不是http1.1的请求/响应模式)
     * <p>
     * 需要开启,但是要注意,因为同一个连接对象netty用的同一个线程处理
     * <p>
     * 解决1,需要接收方handler里再开一个线程池处理信息(目前用这种)
     * 解决2,需要发送方每次通过另一个连接发送,pool连接数=接收方处理线程数(pool目前还没能实现)
     * <p>
     * false:等到响应/超时才放回连接池,如果请求延迟较大,将会阻塞无法发挥最大性能,解决方法是增大连接池
     */
    public static boolean POOL_NIO = true;// tcp链接协议不支持双工才需要false,如http

    private static volatile RPCClientManager instance;

    public static RPCClientManager getInstance() {
        if (instance == null) {
            synchronized (RPCClientManager.class) {
                if (instance == null) {
                    instance = new RPCClientManager();
                }
            }
        }
        return instance;
    }

    private NodePoolManager nodePoolManager;

    private RPCClientManager() {
        nodePoolManager = new NodePoolManager();
        nodePoolManager.initNodePool();
    }

    /**
     * 同步发送,阻塞,
     * <p>
     * 访问延迟大将会导致线程挂起太久,CPU无法跑满,而解决方法只有新建更多线程,性能不好,
     * <p>
     * 如作为主请求路由RPC强烈不建议用
     *
     * @throws InterruptedException
     * @throws RPCException
     */
    public byte[] sendSync(String action, byte[] content) throws RPCException, InterruptedException {
        return sendSync(action, content, RpcTimeout);
    }

    /**
     * 同步发送,阻塞,
     * <p>
     * 访问延迟大将会导致线程挂起太久,CPU无法跑满,而解决方法只有新建更多线程,性能不好,
     * <p>
     * 作为主请求路由RPC强烈不建议用
     *
     * @throws InterruptedException
     * @throws RPCException
     */
    public byte[] sendSync(String action, byte[] content, int timeout) throws RPCException, InterruptedException {
        CallFuture<byte[]> callFuture = sendAsync(action, content, timeout);
        return callFuture.get();
//        ClientPool clientPool = nodePoolManager.chooseClientPool(action);
//        if (clientPool != null) {
//            TCPRouteClient tcpClient = clientPool.getResource();
//            if (tcpClient != null) {
//                try {
//                    Message request = new Message();
//                    request.setId(Message.createID());
//                    request.setContent(content);
//                    if (POOL_NIO) {
//                        clientPool.returnResource(tcpClient);
//                    }
//                    logger.info("sendSync:" + action + "," + request.getId() + "," + tcpClient.getInfo());
//                    return tcpClient.sendSync(request, timeout).getContent();
//                } finally {
//                    if (!POOL_NIO) {
//                        clientPool.returnResource(tcpClient);
//                    }
//                }
//            } else {
//                throw new RPCException("can get client from pool:" + action + "," + clientPool);
//            }
//        } else {
//            logger.error("can no choose pool:" + action);
//            throw new RPCException("can no choose pool:" + action);
//        }
    }

    /**
     * 异步Future,nio
     */
    public CallFuture<byte[]> sendAsync(String action, byte[] content) {
        return sendAsync(action, content, RpcTimeout);
    }

    /**
     * 异步Future,nio
     */
    public CallFuture<byte[]> sendAsync(String action, byte[] content, int timeout) {
        CallFuture<byte[]> callback = CallFuture.<byte[]>newInstance();
        sendAsync(action, content, callback, timeout);
        return callback;
    }

    /**
     * 异步回调,nio
     */
    public boolean sendAsync(String action, byte[] content, Callback<byte[]> callback) {
        return sendAsync(action, content, callback, RpcTimeout);
    }

    /**
     * 异步回调,nio
     */
    public boolean sendAsync(String action, byte[] content, Callback<byte[]> callback, int timeout) {
        ClientPool clientPool = nodePoolManager.chooseClientPool(action);
        if (clientPool != null) {
            TCPRouteClient tcpClient = clientPool.getResource();
            if (tcpClient != null) {
                Message request = new Message();
                request.setId(Message.createID());
                request.setContent(content);

                tcpClient.sendAsync(request, new AsyncCallback(callback, clientPool, tcpClient), timeout);
                if (POOL_NIO) {
                    clientPool.returnResource(tcpClient);
                }
                logger.info("sendAsync:" + action + "," + request.getId() + "," + tcpClient.getInfo());
            } else {
                callback.handleError(new RPCException("can get client from pool:" + action + "," + clientPool));
            }
            return true;
        } else {
            logger.error("can no choose pool:" + action);
            callback.handleError(new RPCException("can no choose pool:" + action));
            return false;
        }
    }

    // 包装异步回调,异步释放pool链接
    private static class AsyncCallback implements Callback<Message> {

        private Callback<byte[]> callback;
        private TCPRouteClient tcpClient;
        private ClientPool clientPool;

        AsyncCallback(Callback<byte[]> callback, ClientPool clientPool, TCPRouteClient tcpClient) {
            this.callback = callback;
            this.clientPool = clientPool;
            this.tcpClient = tcpClient;
        }

        @Override
        public void handleResult(Message result) {
            if (!POOL_NIO) {
                clientPool.returnResource(tcpClient);
            }
            tcpClient = null;
            callback.handleResult(result.getContent());
        }

        @Override
        public void handleError(Throwable error) {
            if (!POOL_NIO) {
                clientPool.returnResource(tcpClient);
            }
            tcpClient = null;
            callback.handleError(error);
        }
    }

}
