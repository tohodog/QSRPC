package org.song.qsrpc.zk;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月15日 下午1:38:25
 * <p>
 * 节点信息
 */
public class NodeInfo {

    @JSONField(serialize = false)
    private String zkIps;// zookeeper 主机
    @JSONField(serialize = false)
    private String zkPath;// zookeeper 数据路径

    private String action;// 服务器处理功能支持多个,如 user,order

    private String ip;// 内网ip/外网IP
    private int port;

    private int coreThread = Runtime.getRuntime().availableProcessors();
    private int weight = 1;
    private boolean ssl;

    public String getZkIps() {
        return zkIps;
    }

    public void setZkIps(String zkIps) {
        this.zkIps = zkIps;
    }

    public String getZkPath() {
        return zkPath;
    }

    public void setZkPath(String zkPath) {
        this.zkPath = zkPath;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
        mark = null;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
        mark = null;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        mark = null;
    }

    public int getCoreThread() {
        return coreThread;
    }

    public void setCoreThread(int coreThread) {
        this.coreThread = coreThread;
        mark = null;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        mark = null;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * 节点唯一标识
     * 加入后面的配置区分改配置重启节点重复问题
     *
     * @return IP + ":" + port
     */
    @JSONField(serialize = false)
    private String mark;

    public String getMark() {
        return mark != null ? mark : (mark = ip + ":" + port + "_" + action + "_" + weight + "_" + coreThread);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NodeInfo) {
            NodeInfo o1 = (NodeInfo) o;
            return this.getMark().equals(o1.getMark());
        }
        return false;
    }
}
