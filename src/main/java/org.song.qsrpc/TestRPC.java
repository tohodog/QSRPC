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

        NodeInfo nodeInfo = NodeRegistry.buildNode(9000);
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return "{\"port\":9000}".getBytes();
            }
        });
        nodeInfo = NodeRegistry.buildNode(9001);
        nodeInfo.setWeight(2);
        NodeLauncher.start(nodeInfo, new MessageListener() {
            @Override
            public byte[] onMessage(Async async, byte[] message) {
                return "{\"port\":9001}".getBytes();
            }
        });

        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("i", "123");
        jsonRequest.put("m", "POST");
        jsonRequest.put("u", "/helloworld/666");
        jsonRequest.put("h", null);
        jsonRequest.put("b", "userid=1&tn=baidu&wd=%E4%B9%96%E4%B9%96%E4%B9%96".getBytes());

        try {

            for (int i = 0; i < 9; i++) {
                byte[] msg_cb = RPCClientManager.getInstance().sendSync("user", jsonRequest.toJSONString().getBytes());

                System.out.println("RPCResult-" + new String(msg_cb));
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
