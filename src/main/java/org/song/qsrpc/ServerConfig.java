package org.song.qsrpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2018年8月6日
 * <p>
 * 服务器配置
 */
public class ServerConfig {

    private final static String PROPRETIES_PATH = "/application.properties";
    private final static Properties properties;

    public final static String KEY_RPC_CFG_LOG = "qsrpc.cfg.log";
    public final static String KEY_RPC_CONNECT_TIMEOUT = "qsrpc.connect.timeout";

    public final static String KEY_RPC_ZK_IPS = "qsrpc.zk.ips";
    public final static String KEY_RPC_ZK_PATH = "qsrpc.zk.path";
    public final static String KEY_RPC_ZK_USERNAME = "qsrpc.zk.username";
    public final static String KEY_RPC_ZK_PASSWORD = "qsrpc.zk.password";

    public final static String KEY_RPC_NACOS_ADDR = "qsrpc.nacos.addr";
    public final static String KEY_RPC_NACOS_SRVNAME = "qsrpc.nacos.srvname";

    public final static String KEY_RPC_NODE_NAME = "qsrpc.node.name";
    public final static String KEY_RPC_NODE_IP = "qsrpc.node.ip";
    public final static String KEY_RPC_NODE_PORT = "qsrpc.node.port";
    public final static String KEY_RPC_NODE_ACTION = "qsrpc.node.action";
    public final static String KEY_RPC_NODE_WEIGHT = "qsrpc.node.weight";
    public final static String KEY_RPC_NODE_ZIP = "qsrpc.node.zip";
    //服务端工作线程数/客户端pool.maxidle
    public final static String KEY_RPC_NODE_THREAD = "qsrpc.node.thread";
    public final static String KEY_RPC_MESSAGE_MAXLEN = "qsrpc.message.maxlen";
    //服务端接收消息是否再分发进线程池(解决消息都在同一个tcp发来导致只有一个线程在处理),如果出现服务端吃不满cpu可尝试配置true
    public final static String KEY_RPC_NODE_REDISTRIBUTE = "qsrpc.node.redistribute";

//    public final static String KEY_SSL_JKS_PATH = "server.ssl.jks.path";
//    public final static String KEY_SSL_JKS_PASSWORD = "server.ssl.jks.password";
//    public final static String KEY_SSL_CERT_PATH = "server.ssl.cert.path";


    public static final RPCConfig RPC_CONFIG = new RPCConfig();

    static {
        properties = new Properties();
        try {
            InputStream is = Object.class.getResourceAsStream(PROPRETIES_PATH);
            if (is != null) properties.load(is);
            RPC_CONFIG.setPrintLog(getBoolean(KEY_RPC_CFG_LOG));

            RPC_CONFIG.setZkIps(getString(KEY_RPC_ZK_IPS));
            RPC_CONFIG.setZkPath(getString(KEY_RPC_ZK_PATH, "/qsrpc"));
            RPC_CONFIG.setZkUserName(getString(KEY_RPC_ZK_USERNAME));
            RPC_CONFIG.setZkPassword(getString(KEY_RPC_ZK_PASSWORD));
            RPC_CONFIG.setNacosAddr(getString(KEY_RPC_NACOS_ADDR));
            RPC_CONFIG.setNacosServiceName(getString(KEY_RPC_NACOS_SRVNAME, "qsrpc"));

            RPC_CONFIG.setClientTimeout(getInt(KEY_RPC_CONNECT_TIMEOUT, 60 * 1000));

            RPC_CONFIG.setNodeName(getString(KEY_RPC_NODE_NAME));
            RPC_CONFIG.setNodeIp(getString(KEY_RPC_NODE_IP));
            RPC_CONFIG.setNodePort(getInt(KEY_RPC_NODE_PORT, 0));
            String node_action = getString(KEY_RPC_NODE_ACTION);
            if (node_action != null) {
                RPC_CONFIG.setNodeAction(node_action.split(","));
            }
            RPC_CONFIG.setNodeWeight(getInt(KEY_RPC_NODE_WEIGHT, 1));
            RPC_CONFIG.setNodeZip(getString(KEY_RPC_NODE_ZIP));
            RPC_CONFIG.setNodeThread(getInt(KEY_RPC_NODE_THREAD, Runtime.getRuntime().availableProcessors() * 2));
            RPC_CONFIG.setNodeMaxLen(getInt(KEY_RPC_MESSAGE_MAXLEN, 1024 * 1024 * 16));
            RPC_CONFIG.setNodeRedistribute(getBoolean(KEY_RPC_NODE_REDISTRIBUTE));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public static int getInt(String key, int def) {
        if (containsKey(key))
            return getInt(key);
        else
            return def;
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static String getString(String key, String def) {
        if (containsKey(key))
            return getString(key);
        else
            return def;
    }

    // 服务器配置必须存在,否则运行异常,防止BUG
    public static String getStringNotnull(String key) {
        String value = properties.getProperty(key);
        if (value == null)
            throw new RuntimeException(key + " property value is null");
        return value;
    }

    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }
}
