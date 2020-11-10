package org.song.qsrpc.send;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private Map<String, List<NodeInfo>> nodeInfoMap = new HashMap<>();// key:action
    private Map<String, NoteRequestInfo> nodeReqInfoMap = new HashMap<>();// key:action

    public void initNodePool() {
        String ips = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_IPS);
        String path = ServerConfig.getString(ServerConfig.KEY_RPC_ZK_PATH);
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

            // 新建新加节点连接池
            Set<String> newNodeMark = new HashSet<>();
            for (NodeInfo nodeInfo : nodeDatas) {
                String mark = nodeInfo.id();
                newNodeMark.add(mark);
                if (!clientPoolMap.containsKey(mark)) {
                    clientPoolMap.put(nodeInfo.id(), buildClientPool(nodeInfo));
                    logger.info("createClientPool:" + mark);
                }
            }
            // 移除不存在节点连接池
            Iterator<String> iterator = clientPoolMap.keySet().iterator();
            while (iterator.hasNext()) {
                String mark = iterator.next();
                if (!newNodeMark.contains(mark)) {
                    ClientPool clientPool = clientPoolMap.get(mark);
                    clientPool.destroy();
                    iterator.remove();
                    logger.info("removeClientPool:" + mark);
                }
            }

            nodeInfoMap.clear();
            nodeReqInfoMap.clear();
            // 把节点按action分组,也就是同样服务功能的服务器放一起
            for (NodeInfo nodeInfo : nodeDatas) {
                String[] actions = nodeInfo.getActions();
                for (String action : actions) {
                    List<NodeInfo> actionList = nodeInfoMap.get(action);
                    if (actionList == null) {
                        actionList = new ArrayList<>();
                        nodeInfoMap.put(action, actionList);
                        nodeReqInfoMap.put(action, new NoteRequestInfo());
                    }
                    //10秒内重启服务器会出现重复节点,处理下
                    if (!actionList.contains(nodeInfo)) {
                        actionList.add(nodeInfo);
                        nodeReqInfoMap.get(action).weightSum += nodeInfo.getWeight();
                    }
                }
            }

        } finally {
            lock.writeLock().unlock();
        }

    }

    // 根据action选择服务器,支持权重
    public ClientPool chooseClientPool(String action) {
        try {
            lock.readLock().lock();
            List<NodeInfo> actionList = nodeInfoMap.get(action);
            if (actionList == null) {// || actionList.size() == 0) {
                logger.info("chooseClientPool: can not find pool - " + action);
                return null;
            }
            NoteRequestInfo noteRequestInfo = nodeReqInfoMap.get(action);

            int nowIndex = 0;
            if (noteRequestInfo.weightSum > 0) {
                nowIndex = (int) (noteRequestInfo.requestCount % noteRequestInfo.weightSum);
            }
            noteRequestInfo.requestCount++;

            int weight = 0;
            for (NodeInfo n : actionList) {
                weight += n.getWeight();
                if (weight > nowIndex) {
                    return clientPoolMap.get(n.id());
                }
            }
            return clientPoolMap.get(actionList.get(0).id());

        } finally {
            lock.readLock().unlock();
        }

    }

    private ClientPool buildClientPool(NodeInfo nodeInfo) {
        PoolConfig poolConfig = new PoolConfig();
//        poolConfig.setMaxIdle(nodeInfo.getCoreThread() * 2);
        ClientPool clientPool = new ClientPool(poolConfig, new ClientFactory(nodeInfo.getIp(), nodeInfo.getPort()));
        return clientPool;

    }

    // 某个action节点组请求统计,方便扩展功能
    // 目前实现按权重分配
    public static class NoteRequestInfo {
        public long requestCount;
        public int weightSum;

    }
}
