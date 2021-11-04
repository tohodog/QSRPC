package org.song.qsrpc.receiver;


import com.alibaba.fastjson.JSON;
import org.song.qsrpc.RPCException;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.discover.*;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月18日 下午7:16:40
 * <p>
 * 节点注册
 */
public class NodeRegistry {

    /**
     * 注册节点
     *
     * @throws RPCException 注册失败
     */
    public static CloseFuture registry(NodeInfo nodeInfo) {

        String nodeIp = nodeInfo.getIp();
        int port = nodeInfo.getPort();
        String[] nodeAction = nodeInfo.getActions();
        //判断不为空
        if (nodeIp == null) throw new NullPointerException(ServerConfig.KEY_RPC_NODE_IP + " is null");
        if (port <= 0 || port > 65535) throw new IllegalStateException(ServerConfig.KEY_RPC_NODE_PORT + " is wrong");
        if (nodeAction == null || nodeAction.length == 0)
            throw new NullPointerException(ServerConfig.KEY_RPC_NODE_ACTION + " is null");

        if (nodeInfo.getName() == null) {
            nodeInfo.setName(nodeIp + ":" + port);
        }

        IDiscover discover = DicoverFactory.newInstance(ServerConfig.RPC_CONFIG);
        if (discover == null)
            throw new NullPointerException(ServerConfig.KEY_RPC_NACOS_ADDR + "/" + ServerConfig.KEY_RPC_ZK_IPS + " is null");

        return reg(discover, nodeInfo);
    }

    private static CloseFuture reg(final IDiscover discover, NodeInfo nodeInfo) {
        if (!discover.register(nodeInfo)) {
            throw new RPCException(discover.getClass().getSimpleName() + " can not register:" + JSON.toJSONString(nodeInfo));
        }
        return new CloseFuture() {
            @Override
            public void close() {
                discover.stop();
            }

            @Override
            public boolean isConnect() {
                return discover.isConnect();
            }
        };
    }

//    private static CloseFuture regNacos(String nacosAddr, String nacosServiceNam, NodeInfo nodeInfo) {
//        final NacosManager nacosManager = new NacosManager(nacosAddr, nacosServiceNam);
//        if (!nacosManager.register(nodeInfo)) {
//            throw new RPCException("NacosManager can not register:" + JSON.toJSONString(nodeInfo));
//        }
//        return new CloseFuture() {
//            @Override
//            public void close() {
//                nacosManager.stop();
//            }
//
//            @Override
//            public boolean isConnect() {
//                return nacosManager.isConnect();
//            }
//        };
//    }
//
//    private static CloseFuture regZK(String zkIps, String zkPath, NodeInfo nodeInfo) {
//        final ZookeeperManager zookeeperManager = new ZookeeperManager(zkIps, zkPath);
//        if (!zookeeperManager.register(nodeInfo)) {
//            throw new RPCException("Zookeeper can not createChildNode:" + JSON.toJSONString(nodeInfo));
//        }
//        return new CloseFuture() {
//            @Override
//            public void close() {
//                zookeeperManager.stop();
//            }
//
//            @Override
//            public boolean isConnect() {
//                return zookeeperManager.isConnect();
//            }
//        };
//    }

    public interface CloseFuture {
        void close();

        boolean isConnect();
    }

    public static NodeInfo buildNode() {
        String nodeIp = ServerConfig.RPC_CONFIG.getNodeIp();
        int port = ServerConfig.RPC_CONFIG.getNodePort();
        String[] nodeAction = ServerConfig.RPC_CONFIG.getNodeAction();
        //判断不为空

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setIp(nodeIp);
        nodeInfo.setPort(port);
        nodeInfo.setActions(nodeAction);

        nodeInfo.setName(ServerConfig.RPC_CONFIG.getNodeName());
        nodeInfo.setWeight((byte) ServerConfig.RPC_CONFIG.getNodeWeight());
        nodeInfo.setCoreThread(ServerConfig.RPC_CONFIG.getNodeThread());
        nodeInfo.setZip(ServerConfig.RPC_CONFIG.getNodeZip());
        return nodeInfo;
    }

}
