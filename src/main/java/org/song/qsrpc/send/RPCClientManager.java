package org.song.qsrpc.send;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.send.cb.CallFuture;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.pool.ClientPool;
import org.song.qsrpc.statistics.StatisticsManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 管理操作类,负责管理操作各个模块
 */
public class RPCClientManager {

    private static final Logger logger = LoggerFactory.getLogger(RPCClientManager.class);

    /**
     * 全局超时,默认60s
     */
    public static final int RpcTimeout;

    static {
        RpcTimeout = ServerConfig.RPC_CONFIG.getClientTimeout();
    }

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
    public byte[] sendSync(String action, byte[] content) throws ExecutionException, InterruptedException, TimeoutException {
        return sendSync(action, content, RpcTimeout);
    }

    public byte[] sendSync(String action, byte[] content, int timeout) throws InterruptedException, TimeoutException, ExecutionException {
        Future<byte[]> callFuture = sendAsync(action, content, timeout);
        return callFuture.get(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 异步Future
     */
    public Future<byte[]> sendAsync(String action, byte[] content) {
        return sendAsync(action, content, RpcTimeout);
    }

    /**
     * 异步Future
     */
    public Future<byte[]> sendAsync(String action, byte[] content, int timeout) {
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

                tcpClient.sendAsync(request, new AsyncCallback(callback, clientPool, tcpClient, request), timeout);
                if (!clientPool.isQueue()) {
                    // 大量请求时,如果这里释放资源,那么请求会全部打出去,堆积在服务端,
                    // 不释放请求就堆积在getResource等待获取资源,都会延时,
                    // 所以要有qps限制,1放在服务端拦截 2放在这里就设置getResource一秒超时,建议1,也就是
                    clientPool.returnResource(tcpClient);
                }
                if (ServerConfig.RPC_CONFIG.isPrintLog()) {
                    logger.debug("sendMessage:" + action + ", id:" + request.getId() + ", channel:" + tcpClient.getInfo());
                }
            } else {
                callback.handleError(new RPCException("Can not get client from pool:" + action + "," + clientPool.toString()));
            }
            return true;
        } else {
            logger.error("Can no find pool:" + action);
            callback.handleError(new RPCException("Can not find pool:" + action));
            return false;
        }
    }

    // 包装异步回调,异步释放pool链接,统计
    private static class AsyncCallback implements Callback<Message> {

        private Callback<byte[]> callback;
        private TCPRouteClient tcpClient;
        private ClientPool clientPool;
        private String ipport;

        AsyncCallback(Callback<byte[]> callback, ClientPool clientPool, TCPRouteClient tcpClient, Message message) {
            this.callback = callback;
            this.clientPool = clientPool;
            this.tcpClient = tcpClient;
            ipport = tcpClient.getIpPort();
            StatisticsManager.getInstance().start(ipport, message);//统计
        }

        @Override
        public void handleResult(Message result) {
            if (clientPool.isQueue()) {
                clientPool.returnResource(tcpClient);
            }
            tcpClient = null;

            StatisticsManager.getInstance().success(ipport, result);//统计
            callback.handleResult(result.getContent());
        }

        @Override
        public void handleError(Throwable error) {
            if (clientPool.isQueue()) {
                clientPool.returnResource(tcpClient);
            }
            tcpClient = null;

            StatisticsManager.getInstance().fail(ipport, error);//统计
            callback.handleError(error);
        }
    }

}
