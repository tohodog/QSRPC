package org.song.qsrpc.send.pool;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.zk.NodeInfo;

/**
 * 连接池对象构造工厂
 */
public class ClientFactory extends BasePoolableObjectFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClientFactory.class);

    private String ip;
    private int port;
    private NodeInfo nodeInfo;

    /**
     * Creates a new instance of ClientChannelFactory.
     *
     * @param ip
     * @param port
     */
    public ClientFactory(String ip, int port) {
        this(ip, port, null);
    }

    public ClientFactory(String ip, int port, NodeInfo nodeInfo) {
        this.ip = ip;
        this.port = port;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public Object makeObject() throws Exception {
        TCPRouteClient client = new TCPRouteClient(ip, port, nodeInfo);
        client.connect();

        if (!client.isConnect()) {
            logger.warn("Making new connection on " + client.getInfo() + " not success");
        }

        logger.info("Making new connection on " + client.getInfo() + " and adding to pool done");

        return client;
    }

    @Override
    public void destroyObject(final Object obj) throws Exception {
        if (obj instanceof TCPRouteClient) {
            final TCPRouteClient ch = (TCPRouteClient) obj;
            ch.close();
            logger.info("Closing channel and destroy connection" + ch.getInfo());
        }
    }

    @Override
    public boolean validateObject(Object obj) {
        if (obj instanceof TCPRouteClient) {
            final TCPRouteClient ch = (TCPRouteClient) obj;
            boolean b = ch.isConnect();
            //logger.info("validate channel isConnect:" + b + " " + ch.getInfo());
            return b;
        }
        return false;
    }

    public String getIpPort() {
        return ip + ":" + port;
    }
}
