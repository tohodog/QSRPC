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

    public static ZookeeperManager registry(NodeInfo nodeInfo) {
        ZookeeperManager zookeeperManager = new ZookeeperManager(nodeInfo.getZkIps(), nodeInfo.getZkPath());
        if (zookeeperManager.createChildNode(nodeInfo.id(), JSON.toJSONString(nodeInfo).getBytes())) {
            return zookeeperManager;
        } else {
            return null;
        }
    }

    public static NodeInfo buildNode() {
        String zkIps = ServerConfig.getStringNotnull(ServerConfig.KEY_RPC_ZK_IPS);
        String zkPath = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_PATH);
        if (zkPath == null) zkPath = "/qsrpc";

        String node_ip = ServerConfig.getStringNotnull(ServerConfig.KEY_RPC_NODE_IP);
        int port = Integer.parseInt(ServerConfig.getStringNotnull(ServerConfig.KEY_RPC_NODE_PORT));
        String node_action = ServerConfig.getString(ServerConfig.KEY_RPC_NODE_ACTION);
        String weight = ServerConfig.getString(ServerConfig.KEY_RPC_NODE_WEIGHT);
        NodeInfo nodeInfo = new NodeInfo();
        if (node_action != null) {
            nodeInfo.setActions(node_action.split(","));
        }
        nodeInfo.setIp(node_ip);
        nodeInfo.setPort(port);
        if (weight != null) {
            nodeInfo.setWeight(Integer.parseInt(weight));
        }
        nodeInfo.setZkIps(zkIps);
        nodeInfo.setZkPath(zkPath);

        nodeInfo.setCoreThread(Runtime.getRuntime().availableProcessors());
        return nodeInfo;
    }

}
