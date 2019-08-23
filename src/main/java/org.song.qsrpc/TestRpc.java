package org.song.qsrpc;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.pool.ClientFactory;
import org.song.qsrpc.send.pool.ClientPool;
import org.song.qsrpc.send.pool.PoolConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年12月26日 下午7:59:14
 * <p>
 * 类说明
 */
public class TestRpc {

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    public static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE,
            DEFAULT_THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024));

    public static int PORT;
    static int count = 10000;

    public static void main(String[] args) throws IOException {
        PORT = ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT);
        NodeLauncher.start(new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return (PORT + "收到").getBytes();
            }
        });

        for (int i = 0; i < 8; i++) {
            // EXECUTOR_SERVICE.submit(syncSINGLE);
//            EXECUTOR_SERVICE.submit(asyncSINGLE);
            EXECUTOR_SERVICE.submit(asyncPOOL);
//             EXECUTOR_SERVICE.submit(syncPOOL);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }

    // 7s
    static Runnable asyncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT));
            client.connect();

            Message msg = new Message();

            for (int i = 0; i < count; i++) {
                msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                client.sendAsync(msg, callback);
            }
        }
    };

    // 8s
    static Runnable syncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT));
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                try {
                    System.out.println("sendAsync id-" + client.sendSync(msg).getId());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };


    //=================使用连接池=================

    static Runnable syncPOOL = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                System.out.println("syncPOOL id-" + sendSyncTest(msg).getId());
            }
        }
    };


    static Runnable asyncPOOL = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                sendAsyncTest(msg, callback);
            }
        }
    };

    static Callback<Message> callback = new Callback<Message>() {

        @Override
        public void handleResult(Message result) {
            System.out.println("callback id-" + result.getId());
        }

        @Override
        public void handleError(Throwable error) {
            error.printStackTrace();
            System.out.println("handleError-" + error);

        }
    };


    // ==================test pool==================

    static ClientPool clientPool = new ClientPool(new PoolConfig(), new ClientFactory("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT)));

    static Message sendSyncTest(Message request) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            try {
                clientPool.returnResource(tcpClient);
                return tcpClient.sendSync(request);
            } catch (RPCException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static void sendAsyncTest(Message request, Callback<Message> callback) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            tcpClient.sendAsync(request, callback);
            clientPool.returnResource(tcpClient);
        }
    }

}
