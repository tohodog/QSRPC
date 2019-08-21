package org.song.qsrpc.send.pool;

import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.zk.NodeInfo;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 连接池
 */
public class ClientPool extends Pool<TCPRouteClient> {

    public ClientPool(PoolConfig poolConfig, ClientFactory factory) {
        super(poolConfig.getPoolConfig(), factory);
    }

}
