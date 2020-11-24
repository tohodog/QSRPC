package test;

import org.song.qsrpc.Log;
import org.song.qsrpc.Message;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.receiver.TCPNodeServer;
import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.send.pool.ClientFactory;
import org.song.qsrpc.send.pool.ClientPool;
import org.song.qsrpc.send.pool.PoolConfig;
import org.song.qsrpc.zk.NodeInfo;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年12月26日 下午7:59:14
 * <p>
 * 类说明
 * tcp链接性能测试,发送和接收都是本程序,所以测试结果低于理论性能
 * cpu:8100
 * TODO 测试需要关闭run日志窗口,360等杀毒软件,非常影响测试性能
 */
public class TestConcurrent {

    private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE,
            DEFAULT_THREAD_POOL_SIZE * 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1024));

    private final static int PORT = 10086;
    private final static int count = 125000;//
    private final static int thread = DEFAULT_THREAD_POOL_SIZE;//x个请求线程
    private final static long len = count * thread;//总共请求
    private final static String zip = "";//gzip snappy
    private final static int timeout = 60_000;


    //加上包头包尾长度12字节,可加大测试带宽
    private static byte[] req = new byte[116];


    public static void main(String[] args) throws IOException {
        NodeInfo info = new NodeInfo();
        info.setPort(PORT);
        new TCPNodeServer(info, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                return message;
            }
        }).start();

        Log.i("start ok!");

        for (int i = 0; i < thread; i++) {
//            EXECUTOR_SERVICE.submit(asyncSINGLE);//异步单链接,这个比较快,因为少了池获取操作,rpc框架考虑不使用连接池,通信只用一个tcp链接
            EXECUTOR_SERVICE.submit(asyncPOOL);//异步线程池

//            EXECUTOR_SERVICE.submit(syncSINGLE);//同步单链接
//            EXECUTOR_SERVICE.submit(syncPOOL);//同步线程池
        }
        temp = System.currentTimeMillis();

    }


    //=================使用连接池=================
    static long temp = 0;
    static volatile long requse;
    static volatile Map<Integer, Long> map = new ConcurrentHashMap<>();

    private static synchronized void requestAdd(int id) {
        requse += (System.currentTimeMillis() - map.get(id));
    }

    //异步POOL
    //4-core-> time:7774 ,qps:128633 ,流量:16079KB/s ,平均请求延时:360
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
    //异步POOL回调
    static Callback<Message> callback = new Callback<Message>() {

        @Override
        public void handleResult(Message res) {
            requestAdd(res.getId());
            if (res.getId() == len) {
                System.out.println("callback id-" + res.getId());
                long use = System.currentTimeMillis() - temp;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println(Runtime.getRuntime().availableProcessors() + "-core-> time:" + use +
                        " ,qps:" + len * 1000 / use +
                        " ,流量:" + len * (res.getContent().length + 12) / 1024 * 1000 / use + "KB/s" +
                        " ,平均请求延时:" + (requse / len)
                );
            }
        }

        @Override
        public void handleError(Throwable error) {
            error.printStackTrace();
            System.out.println("handleError-" + error);
        }
    };


    //同步POOL
    static Runnable syncPOOL = new Runnable() {

        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setContent(req);
                Message res = sendSyncTest(msg);
                requse += (System.currentTimeMillis() - map.get(res.getId()));

//                System.out.println("syncPOOL id-" +res.getId());
                if (res.getId() == len) {
                    System.out.println("callback id-" + res.getId());
                    long use = System.currentTimeMillis() - temp;
                    System.err.println("use time:" + use +
                            " ,qps:" + len * 1000 / use +
                            " ,流量:" + len * (req.length + 12) * 1000 / use / 1024 + "KB/s" +
                            " ,平均请求延时:" + (requse / len));
                }
            }
        }
    };


    // ==================test single==================

    //异步
    //use time:7088 ,qps:141083 ,流量:17635KB/s ,平均请求延时:892
    static Runnable asyncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", PORT);
            client.connect();
            for (int i = 0; i < count; i++) {
                Message msg = new Message();
                msg.setId(Message.createID());
                map.put(msg.getId(), System.currentTimeMillis());
                msg.setContent(req);
                client.sendAsync(msg, callback, timeout);
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
                map.put(msg.getId(), System.currentTimeMillis());
                try {
                    Message res = client.sendSync(msg, timeout);
                    requse += (System.currentTimeMillis() - map.get(res.getId()));

                    if (res.getId() == len) {
                        System.out.println("callback id-" + res.getId());
                        long use = System.currentTimeMillis() - temp;
                        System.err.println("use time:" + use +
                                " ,qps:" + len * 1000 / use +
                                " ,流量:" + len * (req.length + 12) / 1024 * 1000 / use + "KB/s"
                                + " ,平均请求延时:" + (requse / len));
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };


    // ==================建立一个 pool==================

    static ClientPool clientPool = new ClientPool(new PoolConfig(), new ClientFactory("127.0.0.1", PORT, zip));//snappy

    //同步
    static Message sendSyncTest(Message request) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            try {
                request.setId(Message.createID());
                map.put(request.getId(), System.currentTimeMillis());
                clientPool.returnResource(tcpClient);
                return tcpClient.sendSync(request, timeout);
            } catch (RPCException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //异步
    static void sendAsyncTest(Message request, Callback<Message> callback) {
        TCPRouteClient tcpClient = clientPool.getResource();
        if (tcpClient != null) {
            request.setId(Message.createID());
            map.put(request.getId(), System.currentTimeMillis());
            tcpClient.sendAsync(request, callback, timeout);
            clientPool.returnResource(tcpClient);
        }
    }
}
