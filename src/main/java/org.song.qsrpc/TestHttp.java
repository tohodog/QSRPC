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
public class TestHttp {

    public static void main(String[] args) throws Exception {
        ServerConfig.init();

        NodeInfo nodeInfo = NodeRegistry.buildNode(9000);
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(byte[] message) {
                return "{\"port\":9000}".getBytes();
            }
        });
        nodeInfo = NodeRegistry.buildNode(9001);
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(byte[] message) {
                return "{\"port\":9001}".getBytes();
            }
        });

        Message msg = new Message();
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("i", "123");
        jsonRequest.put("m", "POST");
        jsonRequest.put("u", "/helloworld/666");
        jsonRequest.put("h", null);
        jsonRequest.put("b", "userid=1&tn=baidu&wd=%E4%B9%96%E4%B9%96%E4%B9%96".getBytes());
        msg.setJSONObject(jsonRequest);

        try {

            for (int i = 0; i < 10; i++) {
                Message msg_cb = RPCClientManager.getInstance().sendSync("user", msg);

                System.out.println("RPCResult-" + msg_cb.getString());
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
