package org.song.qsrpc.discover;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2021/8/10
 */
public interface IDiscover {

    boolean isConnect();

    void stop();

    boolean register(NodeInfo nodeInfo);

    void watchIndex(Watcher<String> watcher);

    NodeInfo getNode(String nodeId);

    void watchAllNode(Watcher<NodeInfo> watcher);

}
