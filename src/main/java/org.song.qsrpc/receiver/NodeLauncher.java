package org.song.qsrpc.receiver;

import org.song.qsrpc.zk.NodeInfo;
import org.song.qsrpc.zk.ZookeeperManager;

public class NodeLauncher {

    public static NodeContext start(final MessageListener messageListener) {
        return start(NodeRegistry.buildNode(), messageListener);
    }

    public static NodeContext start(final NodeInfo nodeInfo, final MessageListener messageListener) {
        final TCPNodeServer tcpNodeServer = new TCPNodeServer(nodeInfo, messageListener);
        boolean node = tcpNodeServer.start();
        ZookeeperManager zk = null;
        if (node) {
            //注册节点信息
            zk = NodeRegistry.registry(nodeInfo);
            if (zk == null) {
                tcpNodeServer.close();
            }
        }

        final ZookeeperManager finalZk = zk;
        return new NodeContext() {
            @Override
            public void close() {
                if (finalZk != null) finalZk.stop();
                tcpNodeServer.close();
            }

            @Override
            public boolean isConnect() {
                return tcpNodeServer.isConnect() && finalZk != null;
            }
        };
    }

    public interface NodeContext {
        void close();

        boolean isConnect();
    }

}
