package org.song.qsrpc.receiver;

import org.song.qsrpc.RPCException;
import org.song.qsrpc.discover.NodeInfo;

public class NodeLauncher {

    public static NodeContext start(final MessageListener messageListener) {
        return start(NodeRegistry.buildNode(), messageListener);
    }

    public static NodeContext start(final NodeInfo nodeInfo, final MessageListener messageListener) {
        final TCPNodeServer tcpNodeServer = new TCPNodeServer(nodeInfo, messageListener);
        boolean node = tcpNodeServer.start();
        NodeRegistry.CloseFuture closeFuture = null;
        if (node) {
            //注册节点信息
            closeFuture = NodeRegistry.registry(nodeInfo);
        } else {
            throw new RPCException("TCPNodeServer Launcher Fail");
        }

        final NodeRegistry.CloseFuture future = closeFuture;
        return new NodeContext() {
            @Override
            public void close() {
                if (future != null) future.close();
                tcpNodeServer.close();
            }

            @Override
            public boolean isConnect() {
                return tcpNodeServer.isConnect();
            }
        };
    }

    public interface NodeContext {
        void close();

        boolean isConnect();
    }

}
