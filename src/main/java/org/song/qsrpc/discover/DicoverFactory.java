package org.song.qsrpc.discover;

import org.song.qsrpc.RPCConfig;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2021/8/10
 */
public class DicoverFactory {

    public static IDiscover newInstance(RPCConfig config) {
        String nacosAddr = config.getNacosAddr();
        String nacosServiceName = config.getNacosServiceName();
        if (nacosAddr != null) {
            return new NacosManager(nacosAddr, nacosServiceName);
        }

        String ips = config.getZkIps();
        String path = config.getZkPath();
        if (ips != null) {
            return new ZookeeperManager(ips, path);
        }
        return null;
    }
}
