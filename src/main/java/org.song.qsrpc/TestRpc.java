package org.song.qsrpc;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.receiver.TCPNodeServer;
import org.song.qsrpc.send.RPCClientManager;
import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.zk.NodeInfo;

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

    public static void main(String[] args) throws IOException {
        ServerConfig.init();

        PORT = ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT);

        NodeLauncher.start(new MessageListener() {
            @Override
            public byte[] onMessage(byte[] message) {

                return (PORT + "收到").getBytes();
            }
        });

        for (int i = 0; i < 1; i++) {
            // ExecutorManager.submit(syncSINGLE);
            // ExecutorManager.submit(asyncSINGLE);
            EXECUTOR_SERVICE.submit(asyncPOOL);

            // ExecutorManager.submit(asyncPOOL);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

        }

//		for (int i = 0; i < 100000; i++) {
//			msg = new Message();
//			msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
//			System.out.println("sendSync-" + i + " " + RPCClientManager.getInstance().sendSync(msg));
//		}

//			Socket s = new Socket("127.0.0.1", 36587);
//
//			for (int i = 0; i < 1; i++) {
//				s.getOutputStream().write(ConversionUtil.intToBytes(100002));
//				s.getOutputStream().write(ConversionUtil.intToBytes(2));
//				s.getOutputStream().write("{}".getBytes());
//				s.getOutputStream().write(new byte[100000]);
//
//				System.err.println("read" + s.getInputStream().read(new byte[100000]));
//
//			}
        // s.close();
        System.err.println("done");

    }

    static int len = 2400;

    // 7s
    static Runnable asyncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT));
            client.connect();

            Message msg = new Message();

            for (int i = 0; i < len; i++) {
                msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                client.sendAsync(msg, callback);
            }
        }

        Callback<Message> callback = new Callback<Message>() {

            @Override
            public void handleResult(Message result) {
                System.out.println("sendAsync id-" + result.getId());

            }

            @Override
            public void handleError(Throwable error) {
                error.printStackTrace();
                System.out.println("handleError-" + error);

            }
        };
    };

    static int index = 0;
    static Runnable syncPOOL = new Runnable() {
        @Override
        public void run() {
            ++index;

            for (int i = 0; i < len; i++) {
                Message msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                System.out.println("syncPOOL id-" + RPCClientManager.getInstance().sendSyncTest(msg).getId());
            }
        }
    };

    static Runnable asyncPOOL = new Runnable() {

        @Override
        public void run() {

            for (int i = 0; i < len; i++) {
                Message msg = new Message();
                msg.setJSONObject(JSON.parseObject("{\"name\":\"client\"}"));
                RPCClientManager.getInstance().sendAsyncTest(msg, callback);
            }
        }

        Callback<Message> callback = new Callback<Message>() {

            @Override
            public void handleResult(Message result) {
                System.out.println("asyncPOOL id:" + result.getId() + ",c:" + result.getString());

            }

            @Override
            public void handleError(Throwable error) {
                error.printStackTrace();
                System.out.println("handleError-" + error);
            }
        };
    };

    // 8s
    static Runnable syncSINGLE = new Runnable() {

        @Override
        public void run() {
            // tcp长连接
            TCPRouteClient client = new TCPRouteClient("127.0.0.1", ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT));
            client.connect();

            for (int i = 0; i < len; i++) {
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
}
