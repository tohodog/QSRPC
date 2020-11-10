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

    public final static String KEY_RPC_ZK_IPS = "qsrpc.zk.ips";
    public final static String KEY_RPC_ZK_PATH = "qsrpc.zk.path";
    public final static String KEY_RPC_ZK_USERNAME = "qsrpc.zk.username";
    public final static String KEY_RPC_ZK_PASSWORD = "qsrpc.zk.password";

    public final static String KEY_RPC_NODE_IP = "qsrpc.node.ip";
    public final static String KEY_RPC_NODE_PORT = "qsrpc.node.port";
    public final static String KEY_RPC_NODE_ACTION = "qsrpc.node.action";
    public final static String KEY_RPC_NODE_WEIGHT = "qsrpc.node.weight";
    public final static String KEY_RPC_NODE_ZIP = "qsrpc.node.zip";

    public final static String KEY_RPC_CONNECT_TIMEOUT = "qsrpc.connect.timeout";


    public final static String KEY_SSL_JKS_PATH = "server.ssl.jks.path";
    public final static String KEY_SSL_JKS_PASSWORD = "server.ssl.jks.password";
    public final static String KEY_SSL_CERT_PATH = "server.ssl.cert.path";


    public final static Properties properties;

    static {
        properties = new Properties();
        try {
            InputStream is = Object.class.getResourceAsStream(PROPRETIES_PATH);
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getInt(String key) {
        return Integer.valueOf(getString(key));
    }

    public static long getLong(String key) {
        return Long.valueOf(getString(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key));
    }

    // 服务器配置必须存在,否则运行异常,防止BUG
    public static String getString(String key) {
        String value = properties.getProperty(key);
//		if (value == null)
//			throw new RuntimeException(key + " property value is null");
        return value;
    }

    public static boolean containsKey(String key) {
        return properties.containsKey(key);
    }
}
