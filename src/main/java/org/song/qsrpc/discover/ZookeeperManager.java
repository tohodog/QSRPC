package org.song.qsrpc.discover;

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

public class ZookeeperManager {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperManager.class);

    private static final int ZK_SESSION_TIMEOUT = 15000;


    private volatile List<byte[]> nodeDatas = new ArrayList<>();

    private String registryAddress;// IP列表
    private ZooKeeper zookeeper;

    private String rootPath;

    public ZookeeperManager(String registryAddress, String rootPath) {
        this.registryAddress = registryAddress;
        this.rootPath = rootPath;
        zookeeper = connectServer();
    }

    /**
     * 链接ZooKeeper,阻塞,超时15s
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            zk = new ZooKeeper(registryAddress, ZK_SESSION_TIMEOUT, new Watcher() {
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
                logger.info("zookeeper conenct server ok");
            } else {
                logger.error("zookeeper conenct server timeout");
            }
        } catch (Exception e) {
            logger.error("zookeeper conenct server failed", e);
        }
        return zk;
    }

    private WatchNode watchNode;

    public void watchNode(final WatchNode watchNode) {
        this.watchNode = watchNode;
        try {
            checkRootNode();
            List<String> serverList = zookeeper.getChildren(rootPath, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent.watchNode:" + event.getState());
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(watchNode);
                    } else if (event.getState() == Event.KeeperState.SyncConnected) {
                        watchNode(watchNode);
                    }
                }
            });
            logger.info("WatchNode.serverList:" + serverList);
//            List<byte[]> data = new ArrayList<>();
//            for (String server : serverList) {
//                byte[] bytes = zookeeper.getData(rootPath + "/" + server, false, null);
//                data.add(bytes);
//            }
//            this.nodeDatas = data;
//            watchNode.onNodeDataChange(data);
            watchNode.onNodeChange(serverList);
        } catch (Exception e) {
            logger.error("Service discovery failed", e);
        }
    }


    public byte[] getNodeData(String server) throws KeeperException, InterruptedException {
        return zookeeper.getData(rootPath + "/" + server, false, null);
    }

    public List<byte[]> getNodeDatas() {
        return nodeDatas;
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
                if (s.isEmpty()) continue;
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

    public boolean isConnect() {
        return zookeeper.getState() == ZooKeeper.States.CONNECTED;
    }

    public void stop() {
        if (zookeeper != null) {
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                logger.error("zookeeper stop failed", e);
            }
        }
    }

    public interface WatchNode {

        void onNodeChange(List<String> serverList);

    }
}
