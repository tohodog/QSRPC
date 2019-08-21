package org.song.qsrpc.receiver;


import com.alibaba.fastjson.JSON;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.zk.NodeInfo;
import org.song.qsrpc.zk.ZookeeperManager;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月18日 下午7:16:40
 * <p>
 * 节点注册
 */
public class NodeRegistry {

    public static void registry(NodeInfo nodeInfo) {
        ZookeeperManager zookeeperManager = new ZookeeperManager(nodeInfo.getZkIps(), nodeInfo.getZkPath());
        zookeeperManager.createChildNode(nodeInfo.getMark(), JSON.toJSONString(nodeInfo).getBytes());
    }

    public static NodeInfo buildNode(int port) {
        String zkIps = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_IPS);
        String zkPath = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_PATH);

        String node_ip = ServerConfig.getString(ServerConfig.KEY_RPC_NODE_IP);
        if (node_ip == null) node_ip = AddressUtils.getInnetIp();
        String node_action = ServerConfig.getString(ServerConfig.KEY_RPC_NODE_ACTION);

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setAction(node_action);
        nodeInfo.setIp(node_ip);
        nodeInfo.setPort(port);

        nodeInfo.setZkIps(zkIps);
        nodeInfo.setZkPath(zkPath);

        nodeInfo.setCoreThread(Runtime.getRuntime().availableProcessors());
        return nodeInfo;
    }

    public static NodeInfo buildNode() {
        return buildNode(ServerConfig.getInt(ServerConfig.KEY_RPC_NODE_PORT));
    }
}
