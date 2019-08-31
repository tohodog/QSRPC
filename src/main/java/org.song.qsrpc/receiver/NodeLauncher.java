package org.song.qsrpc.receiver;

import org.song.qsrpc.zk.NodeInfo;

public class NodeLauncher {

    public static TCPNodeServer start(final MessageListener messageListener) {
        return start(NodeRegistry.buildNode(), messageListener);
    }

    public static TCPNodeServer start(final NodeInfo nodeInfo, final MessageListener messageListener) {
        final TCPNodeServer tcpNodeServer = new TCPNodeServer(nodeInfo, messageListener);
        tcpNodeServer.start();
        //注册节点信息
        NodeRegistry.registry(nodeInfo);
        return tcpNodeServer;
    }

}
