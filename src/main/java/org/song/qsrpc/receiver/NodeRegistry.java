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
        String zkIps = nodeInfo.getZkIps();
        String nodeIp = nodeInfo.getIp();
        int port = nodeInfo.getPort();
        String[] nodeAction = nodeInfo.getActions();

        //判断不为空
        if (zkIps == null) throw new NullPointerException(ServerConfig.KEY_RPC_ZK_IPS + " is null");
        if (nodeIp == null) throw new NullPointerException(ServerConfig.KEY_RPC_NODE_IP + " is null");
        if (port <= 0 || port > 65535) throw new IllegalStateException(ServerConfig.KEY_RPC_NODE_PORT + " is wrong");
        if (nodeAction == null || nodeAction.length == 0)
            throw new NullPointerException(ServerConfig.KEY_RPC_NODE_ACTION + " is null");

        ZookeeperManager zookeeperManager = new ZookeeperManager(nodeInfo.getZkIps(), nodeInfo.getZkPath());
        if (zookeeperManager.createChildNode(nodeInfo.id(), JSON.toJSONString(nodeInfo).getBytes())) {
            return zookeeperManager;
        } else {
            return null;
        }
    }

    public static NodeInfo buildNode() {
        String zkIps = ServerConfig.RPC_CONFIG.getZkIps();
        String nodeIp = ServerConfig.RPC_CONFIG.getNodeIp();
        int port = ServerConfig.RPC_CONFIG.getNodePort();
        String[] nodeAction = ServerConfig.RPC_CONFIG.getNodeAction();
        //判断不为空

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setZkIps(zkIps);
        nodeInfo.setZkPath(ServerConfig.RPC_CONFIG.getZkPath());

        nodeInfo.setActions(nodeAction);
        nodeInfo.setIp(nodeIp);
        nodeInfo.setPort(port);
        nodeInfo.setWeight((byte) ServerConfig.RPC_CONFIG.getNodeWeight());
        nodeInfo.setCoreThread(ServerConfig.RPC_CONFIG.getNodeThread());
        nodeInfo.setZip(ServerConfig.RPC_CONFIG.getNodeZip());
        return nodeInfo;
    }

}
