package org.song.qsrpc.send.pool;

import org.song.qsrpc.send.TCPRouteClient;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 连接池
 */
public class ClientPool extends Pool<TCPRouteClient> {
    /**
     * tcp连接通信模式:作为大量并发时的一个配置
     * false:默认nio并发发送,可发送超过节点处理极限的qps,大量请求会堆积在服务提供者,默认这个,由服务者来限制qps
     * true:排队,请求-响应,可作为限流降级,但是大量请求会堆积在请求者等待连接池获取,一样会超时
     */
    private boolean queue;
    private ClientFactory factory;

    public ClientPool(PoolConfig poolConfig, ClientFactory factory) {
        this(poolConfig, factory, false);
    }

    public ClientPool(PoolConfig poolConfig, ClientFactory factory, boolean queue) {
        super(poolConfig.getPoolConfig(), factory);
        this.factory = factory;
        this.queue = queue;
    }

    //这里优化下,tcp通信处于非阻塞模式时,不使用连接池,直接new一个使用,可以提高性能,因为少了竞争获取pool操作
    //测试发现更慢了,待解决...
//    private volatile TCPRouteClient tcpRouteClient;
//
//    @Override
//    public TCPRouteClient getResource() {
//        if (isQueue()) {
//            return super.getResource();
//        }
//        if (!factory.validateObject(tcpRouteClient)) {
//            synchronized (this) {
//                if (!factory.validateObject(tcpRouteClient)) {
//                    try {
//                        tcpRouteClient = (TCPRouteClient) factory.makeObject();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//        return tcpRouteClient;
//    }
//
//    @Override
//    public void returnResource(final TCPRouteClient resource) {
//        if (isQueue()) {
//            super.returnResource(resource);
//        }
//    }
//
//    @Override
//    public void destroy() throws RPCException {
//        if (isQueue()) {
//            super.destroy();
//        }
//        try {
//            factory.destroyObject(tcpRouteClient);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public boolean isQueue() {
        return queue;
    }
}
