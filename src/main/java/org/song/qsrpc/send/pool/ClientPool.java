package org.song.qsrpc.send.pool;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.send.TCPRouteClient;
import org.song.qsrpc.discover.NodeInfo;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月1日 下午5:44:24
 * <p>
 * 连接池
 */
public class ClientPool extends Pool<TCPRouteClient> {

    /**
     * queue对tcp pool的影响
     * false:
     * 拿出连接对象发送信息后,马上放回pool,nio设计,连接池只要几个链接就够了(因为TCPClient支持全双工,所以可以同时发消息,不是http1.1的请求/响应模式)
     * <p>
     * 需要开启,但是要注意,因为同一个连接对象netty用的同一个线程处理
     * <p>
     * 解决1,需要接收方handler里再开一个线程池处理信息,但高并发会使处理延时增大超时,因为消息又放回线程池排队了
     * 解决2,需要发送方每次通过另一个连接发送,pool连接数=接收方处理线程数(pool配置先进先出队列)(*已使用*)
     * <p>
     * true:等到响应/超时才放回连接池,如果请求延迟较大,将会阻塞无法发挥最大性能,解决方法是增大连接池
     */

    /**
     * tcp连接通信模式:作为大量并发时的一个配置:
     * <p>
     * False:默认*,nio并发发送,可发送超过节点处理极限的qps,请求会堆积在服务提供者,由服务者来限制qps
     * <p>
     * True:排队,请求-响应,可作为自动限流降级,因为服务节点处理完一个请求才会发送下一个请求,配置pool.size即可自动吃满服务节点的性能
     * 但是请求会堆积在请求者等待连接池获取,一样会超时,可配置pool-whenExhaustedAction抛异常,达到自动限制最高qps效果
     * 当请求延时大时,pool.size大小将会限制性能发挥,根据情况调整size,默认=core*2
     */
    private boolean queue;//
    private NodeInfo nodeInfo;
    private PoolConfig poolConfig;

    public ClientPool(PoolConfig poolConfig, ClientFactory factory) {
        this(poolConfig, factory, null);
    }

    public ClientPool(PoolConfig poolConfig, ClientFactory factory, NodeInfo nodeInfo) {
        super(poolConfig.buildConfig(), factory);
        this.poolConfig = poolConfig;
        if (nodeInfo != null) {
            this.nodeInfo = nodeInfo;
            this.queue = nodeInfo.isQueue();
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public boolean isQueue() {
        return queue;
    }

    public String toString() {
        return "ClientPool(" + nodeInfo.id() + "," + JSON.toJSONString(poolConfig) + ")";
    }

    //待验证功能,和预想不一样
    //tcp通信处于非阻塞模式时,不使用连接池,直接new一个使用,可以提高性能,因为少了竞争获取pool操作
    //但这么做需要服务端收到消息再分发出去,高并发时服务端延迟会增大...
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

}
