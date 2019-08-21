package org.song.qsrpc.receiver;

import org.song.qsrpc.Log;
import org.song.qsrpc.zk.NodeInfo;

public class NodeLauncher {

    public static boolean start(final MessageListener messageListener) {
        return start(NodeRegistry.buildNode(), messageListener);
    }

    public static boolean start(final NodeInfo nodeInfo, final MessageListener messageListener) {
        final TCPNodeServer tcpNodeServer = new TCPNodeServer(nodeInfo, messageListener);
        return tcpNodeServer.start();
    }

}
