package test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.send.RPCClientManager;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.discover.NodeInfo;

import java.util.concurrent.Future;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月20日 下午2:16:41
 * <p>
 * 类说明
 * 需要 application.properties
 */
public class TestRPC extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestRPC(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestRPC.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
//        try {
//            main(null);
//            assertTrue(true);
//        } catch (Exception e) {
//            fail();
//        }
    }

    public static void main(String[] args) throws Exception {
//        ServerConfig.RPC_CONFIG.setNacosAddr("192.168.0.68:6666");
//        ServerConfig.RPC_CONFIG.setNacosServiceName("qsrpc");
//        ServerConfig.RPC_CONFIG.setZkIps("127.0.0.1:2181");
//        ServerConfig.RPC_CONFIG.setZkPath("/qsrpc");

        //open node server 1
        final NodeInfo nodeInfo = NodeRegistry.buildNode();//read application.properties
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return ("Hello! " + nodeInfo.getPort() + " callback -" + new String(message)).getBytes();
            }
        });

        //open node server 2
        NodeInfo nodeInfo2 = new NodeInfo();
        nodeInfo2.setAction("order");
        nodeInfo2.setIp("127.0.0.1");
        nodeInfo2.setPort(10086);
        nodeInfo2.setWeight((byte) 2);

        NodeLauncher.start(nodeInfo2, new MessageListener() {
            @Override
            public byte[] onMessage(final Async async, final byte[] message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        async.callBack(("Hello! 10086 callback -" + new String(message)).getBytes());
                    }
                }).start();
                return null;
            }
        });

        Thread.sleep(3000);

        //open node server 2
        NodeInfo nodeInfo3 = new NodeInfo();
        nodeInfo3.setAction("order");
        nodeInfo3.setIp("127.0.0.1");
        nodeInfo3.setPort(10087);
        nodeInfo3.setWeight((byte) 1);

        NodeLauncher.start(nodeInfo3, new MessageListener() {
            @Override
            public byte[] onMessage(final Async async, final byte[] message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        async.callBack(("Hello! 10087 callback -" + new String(message)).getBytes());
                    }
                }).start();
                return null;
            }
        });

        Thread.sleep(3000);

        //async
        for (int i = 0; i < 9; i++) {
            RPCClientManager.getInstance().sendAsync("user", "user".getBytes(),
                    new Callback<byte[]>() {
                        @Override
                        public void handleResult(byte[] result) {
                            System.out.println("send [user] Result: " + new String(result));
                        }

                        @Override
                        public void handleError(Throwable error) {
                            error.printStackTrace();
                        }
                    });
        }
        System.out.println("send [user] Done");

        //sync
        for (int i = 0; i < 9; i++) {
            Thread.sleep(1000);
            byte[] msg_cb = RPCClientManager.getInstance().sendSync("order", "order".getBytes());
            System.out.println("send [order] Result: " + new String(msg_cb));
        }
        System.out.println("send [order] Done");

        //future
        Future<byte[]> callFuture = RPCClientManager.getInstance().sendAsync("user", "user".getBytes());
        System.out.println("send [user] FutureResult: " + new String(callFuture.get()));

    }
}
