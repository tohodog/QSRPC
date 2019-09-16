package org.song.qsrpc;

import com.alibaba.fastjson.JSONObject;
import org.song.qsrpc.receiver.MessageListener;
import org.song.qsrpc.receiver.NodeLauncher;
import org.song.qsrpc.receiver.NodeRegistry;
import org.song.qsrpc.receiver.TCPNodeServer;
import org.song.qsrpc.send.RPCClientManager;
import org.song.qsrpc.zk.NodeInfo;

import java.io.IOException;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月20日 下午2:16:41
 * <p>
 * 类说明
 */
public class TestRPC {

    public static void main(String[] args) throws Exception {

        //开启两个节点服务器
        NodeInfo nodeInfo = NodeRegistry.buildNode(9000);
        nodeInfo.setAction("user,order");
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return "9000 callback".getBytes();
            }
        });

        NodeInfo nodeInfo2 = NodeRegistry.buildNode(9001);
        nodeInfo2.setWeight(2);
        nodeInfo2.setAction("order");
        NodeLauncher.start(nodeInfo2, new MessageListener() {
            @Override
            public byte[] onMessage(final Async async, byte[] message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        async.callBack("9001 callback".getBytes());
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

        try {

            for (int i = 0; i < 9; i++) {
                byte[] msg_cb = RPCClientManager.getInstance().sendSync("user", jsonRequest.toJSONString().getBytes());
                System.out.println("send [user] Result: " + new String(msg_cb));
            }
            for (int i = 0; i < 9; i++) {
                byte[] msg_cb = RPCClientManager.getInstance().sendSync("order", jsonRequest.toJSONString().getBytes());
                System.out.println("send [order] Result: " + new String(msg_cb));
            }
        } catch (RPCException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
