package org.song.qsrpc.zk;

/**
 * @author song
 * @Email vipqinsong@gmail.com
 * @date 2019年3月15日 下午1:38:25
 * <p>
 * 节点信息
 */
public class NodeInfo {

    private String zkIps;// zookeeper 主机
    private String zkPath;// zookeeper 数据路径

    private String action;// 服务器处理功能支持多个,如 user,order

    private String ip;// 内网ip/外网IP
    private int port;

    private int coreThread = Runtime.getRuntime().availableProcessors();
    private int weight = 1;
    private boolean backup;
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
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getCoreThread() {
        return coreThread;
    }

    public void setCoreThread(int coreThread) {
        this.coreThread = coreThread;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * 节点唯一标识
     *
     * @return IP + ":" + port
     */
    public String getMark() {
        return ip + ":" + port;
    }

}
