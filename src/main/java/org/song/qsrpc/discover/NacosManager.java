package org.song.qsrpc.discover;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2021/4/15
 */
public class NacosManager implements IDiscover {

    private static final Logger logger = LoggerFactory.getLogger(NacosManager.class);


    private final String serverAddr;// IP列表
    private final String serviceName, serviceIndexName;
    private NamingService namingService;

    public NacosManager(String serverAddr, String serviceName) {
        this.serverAddr = serverAddr;
        if (serviceName == null || serviceName.length() == 0)
            this.serviceName = "qsrpc";
        else
            this.serviceName = serviceName;
        serviceIndexName = serviceName + "-index";
        connectServer();
    }

    public void connectServer() {
        stop();
        try {
            namingService = NacosFactory.createNamingService(serverAddr);
        } catch (NacosException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        isConnect();
    }

    @Override
    public boolean register(NodeInfo nodeInfo) {
        if (namingService == null) return false;

        //节点信息
        Instance instanceNode = new Instance();
        instanceNode.setServiceName(nodeInfo.getName());
        instanceNode.setIp(nodeInfo.getIp());
        instanceNode.setPort(nodeInfo.getPort());
        instanceNode.setHealthy(true);
        instanceNode.setWeight(nodeInfo.getWeight());
        Map<String, String> instanceMeta = new HashMap<>();
        instanceMeta.put("nodeInfo", JSON.toJSONString(nodeInfo));
        instanceNode.setMetadata(instanceMeta);
        instanceNode.setClusterName(nodeInfo.id());

        //只保存instanceId,监听此服务列表变化,再去查instanceNode的nodeInfo信息
        Instance instanceIndex = new Instance();
        instanceIndex.setServiceName(nodeInfo.getName());
        instanceIndex.setIp(nodeInfo.getIp());
        instanceIndex.setPort(nodeInfo.getPort());
        instanceIndex.setClusterName(nodeInfo.id());
        try {
            namingService.registerInstance(serviceName, instanceNode);
            namingService.registerInstance(serviceIndexName, instanceIndex);
            logger.info("registerInstance-> " + serviceName + "=" + JSON.toJSONString(instanceNode));
            return true;
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void watchIndex(final Watcher<String> watcher) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceIndexName);
            handleIndex(instances, watcher);
            namingService.subscribe(serviceIndexName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        handleIndex(instances, watcher);
                    }
                }
            });
            logger.info("subscribe-> " + serviceIndexName);
        } catch (NacosException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleIndex(List<Instance> instances, final Watcher<String> watcher) {
        List<String> nodeIndexList = new ArrayList<>();
        for (Instance instance : instances) {
            nodeIndexList.add(instance.getClusterName());
        }
        logger.info("watchIndex:" + nodeIndexList.size() + "->" + JSON.toJSONString(instances));
        watcher.onNodeChange(nodeIndexList);
    }

    @Override
    public NodeInfo getNode(String nodeId) {
        try {
            Instance instance = namingService.selectOneHealthyInstance(serviceName, Arrays.asList(nodeId));
            String nodeInfoStr = instance.getMetadata().get("nodeInfo");
            logger.info("getNode(" + serviceName + "," + nodeId + ")-> " + instance);
            return JSON.parseObject(nodeInfoStr, NodeInfo.class);
        } catch (NacosException e) {
            e.printStackTrace();
            logger.error("getNode(" + serviceName + "," + nodeId + ")", e);
        }
        return null;
    }

    @Override
    public void watchAllNode(final Watcher<NodeInfo> watcher) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            handleChange(instances, watcher);
            namingService.subscribe(serviceName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        handleChange(instances, watcher);
                    }
                }
            });
            logger.info("subscribe-> " + serviceName);
        } catch (NacosException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleChange(List<Instance> instances, final Watcher<NodeInfo> watcher) {
        List<NodeInfo> nodeInfoList = new ArrayList<>();
        for (Instance instance : instances) {
            String nodeInfoStr = instance.getMetadata().get("nodeInfo");
            NodeInfo nodeInfo = JSON.parseObject(nodeInfoStr, NodeInfo.class);
            if (nodeInfo != null) {
                nodeInfoList.add(nodeInfo);
            }
        }
        logger.info("watchAllNode:" + nodeInfoList.size() + "->" + JSON.toJSONString(instances));
        watcher.onNodeChange(nodeInfoList);
    }

    @Override
    public boolean isConnect() {
        return namingService != null && "UP".equals(namingService.getServerStatus());
    }

    @Override
    public void stop() {
        if (namingService != null) {
            try {
                namingService.shutDown();
            } catch (NacosException e) {
                e.printStackTrace();
            }
        }
    }

}