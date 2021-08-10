package org.song.qsrpc.discover;

import org.song.qsrpc.RPCConfig;
import org.song.qsrpc.ServerConfig;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2021/8/10
 */
public class DicoverFactory {

    public static IDiscover newInstance(RPCConfig config) {
        String nacosAddr = ServerConfig.RPC_CONFIG.getNacosAddr();
        String nacosServiceName = ServerConfig.RPC_CONFIG.getNacosServiceName();
        if (nacosAddr != null) {
            return new NacosManager(nacosAddr, nacosServiceName);
        }

        String ips = ServerConfig.RPC_CONFIG.getZkIps();
        String path = ServerConfig.RPC_CONFIG.getZkPath();
        if (ips != null) {
            return new ZookeeperManager(ips, path);
        }
        return null;
    }
}
