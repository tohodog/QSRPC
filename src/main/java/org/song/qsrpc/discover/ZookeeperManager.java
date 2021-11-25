package org.song.qsrpc.discover;

import com.alibaba.fastjson.JSON;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperManager implements IDiscover {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperManager.class);

    private static final int ZK_SESSION_TIMEOUT = 15000;

    private final String registryAddress;// IP列表
    private final String rootPath;

    private ZooKeeper zookeeper;

    public ZookeeperManager(String registryAddress, String rootPath) {
        if (rootPath == null) rootPath = "/qsrpc";
        this.registryAddress = registryAddress;
        this.rootPath = rootPath;
        connectServer();
    }

    /**
     * 链接ZooKeeper,阻塞,超时15s
     */
    public void connectServer() {
        stop();
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            zookeeper = new ZooKeeper(registryAddress, ZK_SESSION_TIMEOUT, new org.apache.zookeeper.Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent.connectServer:" + event.getState());

                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                    if (event.getState() == Event.KeeperState.Disconnected) {
                    }
                }
            });
            if (latch.await(ZK_SESSION_TIMEOUT + 500, TimeUnit.MILLISECONDS)) {
                logger.info("Conenct server ok");
            } else {
                logger.error("Conenct server timeout");
                throw new RuntimeException("Conenct server timeout");
            }
        } catch (Exception e) {
            logger.error("Conenct server failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean register(NodeInfo nodeInfo) {
        return createChildNode(nodeInfo.id(), JSON.toJSONBytes(nodeInfo));
    }


    @Override
    public void watchIndex(final Watcher<String> watcher) {
        try {
            checkRootNode();
            List<String> serverList = zookeeper.getChildren(rootPath, new org.apache.zookeeper.Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent.watchNode:" + event.getState());
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchIndex(watcher);
                    } else if (event.getState() == Event.KeeperState.SyncConnected) {
                        watchIndex(watcher);
                    }
                }
            });
            logger.info("watchIndex:" + serverList.size() + "->" + serverList);
            watcher.onNodeChange(serverList);
        } catch (Exception e) {
            logger.error("Service discovery failed", e);
        }
    }

    @Override
    public NodeInfo getNode(String nodeId) {
        try {
            return JSON.parseObject(getNodeData(nodeId), NodeInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void watchAllNode(final Watcher<NodeInfo> watcher) {
        try {
            checkRootNode();
            List<String> serverList = zookeeper.getChildren(rootPath, new org.apache.zookeeper.Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent.watchNode:" + event.getState());
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchAllNode(watcher);
                    } else if (event.getState() == Event.KeeperState.SyncConnected) {
                        watchAllNode(watcher);
                    }
                }
            });
            List<NodeInfo> data = new ArrayList<>();
            for (String server : serverList) {
                data.add(getNode(server));
            }
            logger.info("watchAllNode:" + serverList.size() + "->" + JSON.toJSONString(data));
            watcher.onNodeChange(data);
        } catch (Exception e) {
            logger.error("Service discovery failed", e);
        }
    }

    public byte[] getNodeData(String server) throws KeeperException, InterruptedException {
        return zookeeper.getData(rootPath + "/" + server, false, null);
    }

    public boolean createChildNode(String nodeName, byte[] datas) {
        return createChildNode(nodeName, datas, ZooDefs.Ids.OPEN_ACL_UNSAFE);
    }

    public boolean createChildNode(String nodeName, byte[] datas, List<ACL> acls) {
        try {
            checkRootNode();
            zookeeper.create(rootPath + "/" + nodeName, datas, acls,
                    CreateMode.EPHEMERAL);
            logger.info("createChildNode->" + nodeName + "=" + new String(datas));
            return true;
        } catch (KeeperException e) {
            logger.error("createChildNode.KeeperException", e);
        } catch (Exception ex) {
            logger.error("createChildNode.Exception", ex);
        }
        return false;
    }

    private void checkRootNode() {
        checkRootNode(ZooDefs.Ids.OPEN_ACL_UNSAFE);
    }

    private void checkRootNode(List<ACL> acls) {
        try {
            String[] arr = rootPath.split("/");
            String path = "";
            for (String s : arr) {
                if (s == null || s.isEmpty()) continue;
                path += "/" + s;
                Stat stat = zookeeper.exists(path, false);
                if (stat == null) {
                    zookeeper.create(path, new byte[0], acls, CreateMode.PERSISTENT);
                    logger.info("createPath:" + path);
                }
            }
        } catch (KeeperException e) {
            logger.error("checkRootNode.KeeperException", e);
        } catch (Exception e) {
            logger.error("checkRootNode.Exception", e);
        }
    }

    public List<ACL> buildACL(String userName, String password) {
        try {
            String auth = userName + ":" + password;
            Id id = new Id("digest", DigestAuthenticationProvider.generateDigest(auth));
            ACL acl = new ACL(ZooDefs.Perms.ALL, id);
            return Collections.singletonList(acl);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isConnect() {
        return zookeeper.getState() == ZooKeeper.States.CONNECTED;
    }

    @Override
    public void stop() {
        if (zookeeper != null) {
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                logger.error("zookeeper stop failed", e);
            }
        }
    }


}
