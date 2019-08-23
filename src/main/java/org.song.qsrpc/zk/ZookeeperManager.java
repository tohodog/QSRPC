package org.song.qsrpc.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperManager {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperManager.class);

    private static final int ZK_SESSION_TIMEOUT = 10000;

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<byte[]> nodeDatas = new ArrayList<>();

    private String registryAddress;// IP列表
    private ZooKeeper zookeeper;

    private String rootPath;

    public ZookeeperManager(String registryAddress, String rootPath) {
        this.registryAddress = registryAddress;
        this.rootPath = rootPath;
        zookeeper = connectServer();
		checkRootNode();
	}

    /**
     * 链接ZooKeeper,阻塞,超时10s
     */
    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent:" + event.getState());

                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                    if (event.getState() == Event.KeeperState.Disconnected) {
                    }
                }
            });
            if (latch.await(ZK_SESSION_TIMEOUT * 11 / 10, TimeUnit.MILLISECONDS)) {
                logger.info("zookeeper conenct server ok");
            } else {
                logger.error("zookeeper conenct server timeout");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("zookeeper conenct server failed", e);
        }
        return zk;
    }

    public void watchNode(final WatchNode watchNode) {
        try {
            List<String> serverList = zookeeper.getChildren(rootPath, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    logger.info("WatchedEvent:" + event.getState());
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(watchNode);
                    }
                }
            });

            List<byte[]> data = new ArrayList<>();
            for (String server : serverList) {
                byte[] bytes = zookeeper.getData(rootPath + "/" + server, false, null);
                data.add(bytes);
            }
            this.nodeDatas = data;

            watchNode.onNodeDataChange(data);
            // logger.info("Service discovery triggered updating connected server node, node
            // data: {}", dataList);
        } catch (KeeperException | InterruptedException e) {
            logger.error("Service discovery failed", e);
        }
    }

    public interface WatchNode {
        void onNodeDataChange(List<byte[]> nodeDatas);
    }

    public List<byte[]> getNodeDatas() {
        return nodeDatas;
    }

    public boolean createChildNode(String nodeName, byte[] datas) {
        try {
            checkRootNode();
            zookeeper.create(rootPath + "/" + nodeName, datas, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("createChildNode:" + nodeName + "=" + new String(datas));
            return true;
        } catch (KeeperException e) {
            logger.error("KeeperException", e);
        } catch (InterruptedException ex) {
            logger.error("InterruptedException", ex);
        }
        return false;
    }

    private void checkRootNode() {
        try {
            Stat s = zookeeper.exists(rootPath, false);
            if (s == null) {
                zookeeper.create(rootPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            logger.error("KeeperException", e);
        } catch (InterruptedException e) {
            logger.error("InterruptedException", e);
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
