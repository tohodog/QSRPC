package org.song.qsrpc.send;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import org.song.qsrpc.ServerConfig;
import org.song.qsrpc.send.pool.ClientFactory;
import org.song.qsrpc.send.pool.ClientPool;
import org.song.qsrpc.send.pool.PoolConfig;
import org.song.qsrpc.zk.NodeInfo;
import org.song.qsrpc.zk.ZookeeperManager;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月18日 下午3:16:41
 * <p>
 * 链接zookeeper,建立连接池
 */
public class NodePoolManager {

    private static final Logger logger = LoggerFactory.getLogger(NodePoolManager.class);

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private ZookeeperManager zookeeperManager;

    // 所有节点的连接池
    private Map<String, ClientPool> clientPoolMap = new HashMap<>();// key:节点唯一标识

    // 按action分组的节点
    private Map<String, ActionNodeContext> actionNodeContextMap = new HashMap<>();// key:action
    private Map<String, NodeInfo> ipNodeMap = new HashMap<>();// key: ip:port

    public void initNodePool() {
        String ips = ServerConfig.getStringNotnull(ServerConfig.KEY_RPC_ZK_IPS);
        String path = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_PATH);
        if (path == null) path = "/qsrpc";
        zookeeperManager = new ZookeeperManager(ips, path);
        zookeeperManager.watchNode(new ZookeeperManager.WatchNode() {
            //监听节点信息
            @Override
            public void onNodeDataChange(List<byte[]> nodeDatas) {
                try {
                    List<NodeInfo> nodeInfos = new ArrayList<>();
                    for (byte[] bytes : nodeDatas) {
                        try {
                            NodeInfo nodeInfo = JSON.parseObject(new String(bytes), NodeInfo.class);
                            if (nodeInfo != null) nodeInfos.add(nodeInfo);
                        } catch (Exception e) {
                            logger.error("onNodeDataChange.parseObject", e);
                        }
                    }
                    logger.info("onNodeDataChange->" + nodeInfos.size() + "=" + JSON.toJSONString(nodeInfos));
                    onNodeChange(nodeInfos);
                } catch (Exception e) {
                    logger.error("onNodeDataChange.ERROR", e);
                }
            }
        });
    }

    private void onNodeChange(List<NodeInfo> nodeDatas) {
        try {
            lock.writeLock().lock();

            // 新建新的节点连接池
            Set<String> newNodeSet = new HashSet<>();
            for (NodeInfo nodeInfo : nodeDatas) {
                String id = nodeInfo.id();
                newNodeSet.add(id);
                if (!clientPoolMap.containsKey(id)) {
                    clientPoolMap.put(nodeInfo.id(), buildClientPool(nodeInfo));
                    logger.info("createClientPool:" + id);
                }
            }
            // 移除不存在节点连接池
            Iterator<String> iterator = clientPoolMap.keySet().iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                if (!newNodeSet.contains(id)) {
                    ClientPool clientPool = clientPoolMap.get(id);
                    clientPool.destroy();
                    iterator.remove();
                    logger.info("removeClientPool:" + id);
                }
            }

            actionNodeContextMap.clear();
            ipNodeMap.clear();
            // 把节点按action分组,也就是同样服务功能的服务器放一起
            for (NodeInfo nodeInfo : nodeDatas) {
                ipNodeMap.put(nodeInfo.getIp() + ":" + nodeInfo.getPort(), nodeInfo);///按ip映射
                String[] actions = nodeInfo.getActions();
                for (String action : actions) {
                    ActionNodeContext actionNodeContext = actionNodeContextMap.get(action);
                    if (actionNodeContext == null) {
                        actionNodeContext = new ActionNodeContext();
                        ActionNodeContext old = actionNodeContextMap.put(action, actionNodeContext);
                        if (old != null) actionNodeContext = old;
                    }
                    actionNodeContext.addNode(nodeInfo);
                }
            }
            //初始化权重数据
            for (Map.Entry<String, ActionNodeContext> e : actionNodeContextMap.entrySet()) {
                e.getValue().initWeight();
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    // 根据action/ip选择服务器,支持权重
    public ClientPool chooseClientPool(String action) {
        try {
            lock.readLock().lock();
            ActionNodeContext actionNodeContext = actionNodeContextMap.get(action);
            if (actionNodeContext == null) {
                if (ipNodeMap.containsKey(action)) {//根据ip选择
                    return clientPoolMap.get(ipNodeMap.get(action).id());
                }
                logger.info("chooseClientPool: can not find pool - " + action);
                return null;
            }
            return clientPoolMap.get(actionNodeContext.nextNode().id());

        } finally {
            lock.readLock().unlock();
        }

    }

    private ClientPool buildClientPool(NodeInfo nodeInfo) {
        PoolConfig poolConfig = new PoolConfig();
        int coreThread = nodeInfo.getCoreThread();
        if (coreThread <= 0) coreThread = 8;
        poolConfig.setMaxIdle(coreThread);
        poolConfig.setNumTestsPerEvictionRun(poolConfig.getMaxIdle());
        if (nodeInfo.isQueue())//请求-响应模式,pool.get()不进行等待,因为会自动吃满qps,没有空闲对象抛异常可以保证请求延时小
            poolConfig.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
        ClientPool clientPool = new ClientPool(poolConfig, new ClientFactory(nodeInfo.getIp(), nodeInfo.getPort()), nodeInfo.isQueue());
        return clientPool;
    }

    // 某个action节点组请求统计,方便扩展功能
    // 目前实现按权重分配
    public static class ActionNodeContext {
        private final List<NodeInfo> nodeInfos = new ArrayList<>();

        private short[] indexMap;//下标为weight,值为nodeInfos对应的节点index,提升选择性能
        private volatile int weightIndex = -1;
        private int weightSum;

        public void addNode(NodeInfo nodeInfo) {
            if (!nodeInfos.contains(nodeInfo)) {
                nodeInfos.add(nodeInfo);
            }
        }

        public void removeNode(NodeInfo nodeInfo) {
            nodeInfos.remove(nodeInfo);
        }

        //根据权重获取节点
        public NodeInfo nextNode() {
            if (nodeInfos.size() == 1) return nodeInfos.get(0);
            if (nodeInfos.size() == 0) return null;
            return nodeInfos.get(indexMap[nextIndex()]);
        }

        //加锁,并发有安全问题
        private synchronized int nextIndex() {
            weightIndex++;
            if (weightIndex >= weightSum) weightIndex = 0;
            return weightIndex;
        }

        //刷新权重映射
        private void initWeight() {
            weightSum = 0;
            for (NodeInfo nodeInfo : nodeInfos) weightSum += nodeInfo.getWeight();
            indexMap = new short[weightSum];

            short index = 0;
            int offset = 0;
            for (NodeInfo nodeInfo : nodeInfos) {
                for (int i = 0; i < nodeInfo.getWeight(); i++) {
                    indexMap[i + offset] = index;
                }
                offset += nodeInfo.getWeight();
                index++;
            }
        }
    }
}
