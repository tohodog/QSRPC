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

    public final static String KEY_RPC_ZK_IPS = "rpc.zk.ips";
    public final static String KEY_RPC_ZK_PATH = "rpc.zk.path";

    public final static String KEY_RPC_NODE_IP = "rpc.node.ip";
    public final static String KEY_RPC_NODE_PORT = "rpc.node.port";
    public final static String KEY_RPC_NODE_ACTION = "rpc.node.action";


    public final static String KEY_SSL_JKS_PATH = "server.ssl.jks.path";
    public final static String KEY_SSL_JKS_PASSWORD = "server.ssl.jks.password";
    public final static String KEY_SSL_CERT_PATH = "server.ssl.cert.path";


    public static Properties properties;

    static {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() throws IOException {
        properties = new Properties();
        InputStream is = Object.class.getResourceAsStream(PROPRETIES_PATH);
        properties.load(is);
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
