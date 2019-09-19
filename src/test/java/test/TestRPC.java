package test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.receiver.TCPNodeServer;
import org.song.qsrpc.send.RPCClientManager;
import org.song.qsrpc.send.cb.Callback;
import org.song.qsrpc.zk.NodeInfo;

import java.io.IOException;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月20日 下午2:16:41
 * <p>
 * 类说明
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
        try {
            main(null);
            assertTrue(true);
        } catch (Exception e) {
            fail();
        }
    }

    public static void main(String[] args) throws Exception {

        //开启两个节点服务器
        NodeInfo nodeInfo = NodeRegistry.buildNode(8848);
        nodeInfo.setAction("user,order");
        nodeInfo.setWeight(2);
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return ("Hello! 8848 callback -" + JSON.parseObject(new String(message)).getString("m")).getBytes();
            }
        });

        NodeInfo nodeInfo2 = NodeRegistry.buildNode(8844);
        nodeInfo2.setWeight(1);
        nodeInfo2.setAction("order");
        NodeLauncher.start(nodeInfo2, new MessageListener() {
            @Override
            public byte[] onMessage(final Async async, final byte[] message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        async.callBack(("Hello! 8844 callback -" + JSON.parseObject(new String(message)).getString("m")).getBytes());
                    }
                }).start();
                return null;
            }
        });

        //发送消息（转发http）
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("i", "192.168.1.1");
        jsonRequest.put("m", "POST");
        jsonRequest.put("u", "/helloworld/666");
        jsonRequest.put("h", null);
        jsonRequest.put("b", "userid=1&tn=baidu&wd=%E4%B9%96%E4%B9%96%E4%B9%96".getBytes());

        for (int i = 0; i < 9; i++) {
            RPCClientManager.getInstance().sendAsync("user", jsonRequest.toJSONString().getBytes(),
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

        jsonRequest.put("m", "GET");

        for (int i = 0; i < 9; i++) {
            Thread.sleep(1000);

            byte[] msg_cb = RPCClientManager.getInstance().sendSync("order", jsonRequest.toJSONString().getBytes());
            System.out.println("send [order] Result: " + new String(msg_cb));
        }
        System.out.println("send [order] Done");


    }

}
