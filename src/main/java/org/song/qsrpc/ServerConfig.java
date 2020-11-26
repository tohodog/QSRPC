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
    public final static String KEY_RPC_CFG_LOG = "qsrpc.cfg.log";

    public final static String KEY_RPC_ZK_IPS = "qsrpc.zk.ips";
    public final static String KEY_RPC_ZK_PATH = "qsrpc.zk.path";
    public final static String KEY_RPC_ZK_USERNAME = "qsrpc.zk.username";
    public final static String KEY_RPC_ZK_PASSWORD = "qsrpc.zk.password";

    public final static String KEY_RPC_NODE_IP = "qsrpc.node.ip";
    public final static String KEY_RPC_NODE_PORT = "qsrpc.node.port";
    public final static String KEY_RPC_NODE_ACTION = "qsrpc.node.action";
    public final static String KEY_RPC_NODE_WEIGHT = "qsrpc.node.weight";
    public final static String KEY_RPC_NODE_ZIP = "qsrpc.node.zip";
    //服务端工作线程数/客户端pool.maxidle
    public final static String KEY_RPC_NODE_THREAD = "qsrpc.node.thread";
    //服务端接收消息是否再分发进线程池(解决消息都在同一个tcp发来导致只有一个线程在处理),如果出现服务端吃不满cpu可尝试配置true
    public final static String KEY_RPC_NODE_REDISTRIBUTE = "qsrpc.node.redistribute";

    public final static String KEY_RPC_CONNECT_TIMEOUT = "qsrpc.connect.timeout";
    public final static String KEY_RPC_MESSAGE_MAXLEN = "qsrpc.message.maxlen";

    public final static String KEY_SSL_JKS_PATH = "server.ssl.jks.path";
    public final static String KEY_SSL_JKS_PASSWORD = "server.ssl.jks.password";
    public final static String KEY_SSL_CERT_PATH = "server.ssl.cert.path";


    public final static Properties properties;

    public static final boolean VALUE_LOG;
    public static final int VALUE_MAXLEN;
    public static final boolean VALUE_REDISTRIBUTE;

    static {
        properties = new Properties();
        try {
            InputStream is = Object.class.getResourceAsStream(PROPRETIES_PATH);
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        VALUE_LOG = getBoolean(ServerConfig.KEY_RPC_CFG_LOG);
        VALUE_REDISTRIBUTE = getBoolean(ServerConfig.KEY_RPC_NODE_REDISTRIBUTE);
        VALUE_MAXLEN = Integer.parseInt(getString(ServerConfig.KEY_RPC_MESSAGE_MAXLEN, 1024 * 1024 * 32 + ""));
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
