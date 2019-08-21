package org.song.qsrpc.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.receiver.TCPNodeServer;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.pool.ClientFactory;
import org.song.qsrpc.send.pool.ClientPool;
import org.song.qsrpc.send.pool.PoolConfig;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 管理操作类,负责管理操作各个模块
 */
public class RPCClientManager {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientManager.class);

    public static final int ReadTimeout = 18 * 1000;

    /**
     * true:
     * 拿出连接对象发送信息后,马上放回pool,nio设计,连接池只要几个链接就够了(因为TCPClient支持全双工,所以可以同时发消息,不是http1.1的请求/响应模式)
     * <p>
     * 需要开启,但是要注意,因为同一个连接对象netty用的同一个线程处理
     * <p>
     * 解决1,需要接收方handler里再开一个线程池处理信息(目前用这种)
     * 解决2,需要发送方每次通过另一个连接发送,pool连接数=接收方处理线程数(pool不好处理)
     * <p>
     * false:等到响应/超时才放回连接池,如果请求延迟较大,将会阻塞无法发挥最大性能,解决方法是增大连接池
     */
    public static boolean POOL_NIO = true;// tcp链接不支持双工才需要false,如http

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
     * 路由RPC不建议用
     *
     * @throws InterruptedException
     * @throws RPCException
     */
    public Message sendSync(String action, Message request) throws RPCException, InterruptedException {
        ClientPool clientPool = nodePoolManager.chooseClientPool(action);
        if (clientPool != null) {
            TCPRouteClient tcpClient = clientPool.getResource();
            if (tcpClient != null) {
                try {
                    if (POOL_NIO) {
                        clientPool.returnResource(tcpClient);
                    }
                    return tcpClient.sendSync(request);
                } finally {
                    if (!POOL_NIO)
                        clientPool.returnResource(tcpClient);
                    logger.info("sendSync:" + action + "," + request.getId() + "," + tcpClient.getInfo());
                }
            } else {
                throw new RPCException("can get client from pool:" + action + "," + clientPool);
            }
        } else {
            logger.error("can no choose pool:" + action);
            throw new RPCException("can no choose pool:" + action);
        }
    }

    /**
     * 异步发送,nio
     */
    public boolean sendAsync(String action, Message request, Callback<Message> callback) {
        ClientPool clientPool = nodePoolManager.chooseClientPool(action);
        if (clientPool != null) {
            TCPRouteClient tcpClient = clientPool.getResource();
            if (tcpClient != null) {
                if (POOL_NIO) {
                    tcpClient.sendAsync(request, callback);
                    clientPool.returnResource(tcpClient);
                } else {
                    tcpClient.sendAsync(request, new AsyncCallback(callback, clientPool, tcpClient));
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

        private Callback<Message> callback;
        private TCPRouteClient tcpClient;
        private ClientPool clientPool;

        AsyncCallback(Callback<Message> callback, ClientPool clientPool, TCPRouteClient tcpClient) {
            this.callback = callback;
            this.clientPool = clientPool;
            this.tcpClient = tcpClient;
        }

        @Override
        public void handleResult(Message result) {
            clientPool.returnResource(tcpClient);
            tcpClient = null;
            callback.handleResult(result);
        }

        @Override
        public void handleError(Throwable error) {
            clientPool.returnResource(tcpClient);
            tcpClient = null;
            callback.handleError(error);
        }
    }

    // ==================test==================

    ClientPool clientPool = new ClientPool(new PoolConfig(), new ClientFactory("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT)));

    public Message sendSyncTest(Message request) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            try {
                if (POOL_NIO) {
                    clientPool.returnResource(tcpClient);
                }
                return tcpClient.sendSync(request);
            } catch (RPCException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (!POOL_NIO)
                    clientPool.returnResource(tcpClient);
            }
        }
        return null;
    }

    public void sendAsyncTest(Message request, Callback<Message> callback) {

        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            if (POOL_NIO) {
                tcpClient.sendAsync(request, callback);
                clientPool.returnResource(tcpClient);
            } else {
                tcpClient.sendAsync(request, new AsyncCallback(callback, clientPool, tcpClient));
            }

        }
    }

}
