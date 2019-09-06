package org.song.qsrpc;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.receiver.TCPNodeServer;
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
 * tcp链接性能测试
 */
public class TestConcurrent {

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE,
            DEFAULT_THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024));

    private final static int PORT;
    private final static int count = 1;//请求一万次
    private final static int thread = 1;

    static {
        PORT = ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT);
    }

    public static void main(String[] args) throws IOException {
        new TCPNodeServer(NodeRegistry.buildNode(), new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return (PORT + "收到").getBytes();
            }
        }).start();

        Log.i("start ok!");

        for (int i = 0; i < thread; i++) {
            // EXECUTOR_SERVICE.submit(asyncSINGLE);//异步单链接
//            EXECUTOR_SERVICE.submit(syncSINGLE);//同步单链接

            EXECUTOR_SERVICE.submit(asyncPOOL);//异步线程池
//             EXECUTOR_SERVICE.submit(syncPOOL);//同步线程池
        }
    }

    // 7s
    static Runnable asyncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", PORT);
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setId(Message.createID());
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                client.sendAsync(msg, callback, 10000);
            }
        }
    };

    // 8s
    static Runnable syncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", PORT);
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setId(Message.createID());
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                try {
                    System.out.println("sendAsync id-" + client.sendSync(msg, 10000).getId());
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

    static ClientPool clientPool = new ClientPool(new PoolConfig(), new ClientFactory("127.0.0.1", PORT));

    static Message sendSyncTest(Message request) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            try {
                request.setId(Message.createID());
                clientPool.returnResource(tcpClient);
                return tcpClient.sendSync(request, 10000);
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
            request.setId(Message.createID());
            tcpClient.sendAsync(request, callback, 10000);
            clientPool.returnResource(tcpClient);
        }
    }

}
