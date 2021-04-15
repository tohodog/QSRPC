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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2021/4/15
 */
public class NacosManager {

    private static final Logger logger = LoggerFactory.getLogger(NacosManager.class);


    private final String serverAddr;// IP列表
    private final String serviceName;
    private NamingService namingService;
    private WatchNode watchNode;

    public NacosManager(String serverAddr, String serviceName) {
        this.serverAddr = serverAddr;
        if (serviceName == null || serviceName.length() == 0)
            this.serviceName = "qsrpc";
        else
            this.serviceName = serviceName;
        try {
            connectServer();
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    public NamingService connectServer() throws NacosException {
        return namingService = NacosFactory.createNamingService(serverAddr);
    }

    public boolean register(NodeInfo nodeInfo) {
        if (namingService == null) return false;
        Instance instance = new Instance();

        instance.setServiceName(nodeInfo.getName());
        instance.setIp(nodeInfo.getIp());
        instance.setPort(nodeInfo.getPort());
        instance.setHealthy(true);
        instance.setWeight(nodeInfo.getWeight());

        Map<String, String> instanceMeta = new HashMap<>();
        instanceMeta.put("nodeInfo", JSON.toJSONString(nodeInfo));
        instance.setMetadata(instanceMeta);
        try {
            namingService.registerInstance(serviceName, instance);
            logger.info("registerInstance-> " + serviceName + "=" + JSON.toJSONString(instance));
            return true;
        } catch (NacosException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void watchNode(final WatchNode watchNode) {
        this.watchNode = watchNode;
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            handleChange(instances, watchNode);
            namingService.subscribe(serviceName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        handleChange(instances, watchNode);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChange(List<Instance> instances, final WatchNode watchNode) {
        logger.info("watchNode-> " + JSON.toJSONString(instances));

        List<NodeInfo> nodeInfoList = new ArrayList<>();
        for (Instance instance : instances) {
            String nodeInfoStr = instance.getMetadata().get("nodeInfo");
            NodeInfo nodeInfo = JSON.parseObject(nodeInfoStr, NodeInfo.class);
            if (nodeInfo != null) {
                nodeInfoList.add(nodeInfo);
            }
        }
        watchNode.onNodeChange(nodeInfoList);
    }

    public boolean isConnect() {
        return namingService != null;
    }

    public void stop() {
        if (namingService != null) {
            try {
                namingService.shutDown();
            } catch (NacosException e) {
                e.printStackTrace();
            }
        }
    }

    public interface WatchNode {

        void onNodeChange(List<NodeInfo> serverList);

    }
}