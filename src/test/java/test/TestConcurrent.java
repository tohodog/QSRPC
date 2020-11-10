package test;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.Log;
import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
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
 * tcp链接性能测试,发送和接收都是本程序,所以并发结果*2=理论性能
 */
public class TestConcurrent {

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE,
            DEFAULT_THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024));

    private final static int PORT;
    private final static int count = 12500;//
    private final static int thread = 8;//8个线程
    private final static long len = count * thread;//总共百万请求


    //加上包头包尾一个消息长度128字节
    private static byte[] req = new byte[116];
    private static byte[] res = req;


    static {
        PORT = ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT);
    }

    public static void main(String[] args) throws IOException {
        new TCPNodeServer(NodeRegistry.buildNode(), new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return res;
            }
        }).start();

        Log.i("start ok!");

        for (int i = 0; i < thread; i++) {
//            EXECUTOR_SERVICE.submit(asyncSINGLE);//异步单链接,这个比较快,rpc框架考虑不使用连接池,通信只用一个tcp链接
//            EXECUTOR_SERVICE.submit(asyncPOOL);//异步线程池

//            EXECUTOR_SERVICE.submit(syncSINGLE);//同步单链接
            EXECUTOR_SERVICE.submit(syncPOOL);//同步线程池
        }
        temp = System.currentTimeMillis();

    }

    //异步
    //use time:15594 ,qos:64127 ,流量:8015KB/s
    static Runnable asyncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", PORT);
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setId(Message.createID());
                msg.setContent(req);
                client.sendAsync(msg, callback, 10000);
            }
        }
    };

    //同步
    static Runnable syncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", PORT);
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setId(Message.createID());
                msg.setContent(req);
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
    static long temp = 0;

    //异步
    //use time:18666 ,qos:53573 ,流量:6696KB/s
    static Runnable asyncPOOL = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setContent(req);
                sendAsyncTest(msg, callback);
            }
        }
    };

    static Callback<Message> callback = new Callback<Message>() {

        @Override
        public void handleResult(Message res) {
            if (res.getId() == len) {
                System.out.println("callback id-" + res.getId());
                long use = System.currentTimeMillis() - temp;
                System.out.println("use time:" + use +
                        " ,qps:" + len * 1000 / use +
                        " ,流量:" + len * (req.length + 12) / 1024 * 1000 / use + "KB/s");
            }
        }

        @Override
        public void handleError(Throwable error) {
            error.printStackTrace();
            System.out.println("handleError-" + error);
        }
    };


    //同步
    static Runnable syncPOOL = new Runnable() {

        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setContent(req);
                Message res = sendSyncTest(msg);
//                System.out.println("syncPOOL id-" +res.getId());
                if (res.getId() == len) {
                    System.out.println("callback id-" + res.getId());
                    long use = System.currentTimeMillis() - temp;
                    System.out.println("use time:" + use +
                            " ,qps:" + len * 1000 / use +
                            " ,流量:" + len * (req.length + 12) * 1000 / use / 1024 + "KB/s");
                }
            }
        }
    };
    // ==================test pool==================

    static ClientPool clientPool = new ClientPool(new PoolConfig(), new ClientFactory("127.0.0.1", PORT,"gzip"));//snappy

    static Message sendSyncTest(Message request) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            try {
                request.setId(Message.createID());
                clientPool.returnResource(tcpClient);
                return tcpClient.sendSync(request, 10_000);
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
            tcpClient.sendAsync(request, callback, 10_000);
            clientPool.returnResource(tcpClient);
        }
    }

}
