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

    private String[] actions;// 服务器处理功能支持多个,如 user,order

    private String ip;// 内网ip/外网IP
    private int port;

    private String zip;// 压缩 snappy gzip
    /**
     * tcp连接通信模式:作为大量并发时的一个配置
     * false:默认并发发送,可发送超过节点处理极限的qps,大量请求会堆积在服务提供者,默认这个,由服务者来限制qps
     * true:排队,请求-响应,可作为限流降级,但是大量请求会堆积在请求者等待连接池获取,一样会超时
     */
    private boolean queue;

    private int coreThread = Runtime.getRuntime().availableProcessors();//这个决定链接的tcp数量
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


    public String[] getActions() {
        return actions;
    }

    public void setActions(String[] actions) {
        this.actions = actions;
    }

    @JSONField(serialize = false)
    public void setAction(String action) {
        this.actions = action.split(",");
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

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public boolean isQueue() {
        return queue;
    }

    public void setQueue(boolean queue) {
        this.queue = queue;
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

    private long time = System.currentTimeMillis();

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    /**
     * 节点唯一标识
     * 加入后面的time区分改配置重启节点重复问题
     *
     * @return IP + ":" + port
     */
    @JSONField(serialize = false)
    private transient String mark;

    public String id() {
        return mark != null ? mark : (mark = ip + ":" + port + "_" + time);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NodeInfo) {
            NodeInfo o1 = (NodeInfo) o;
            return this.id().equals(o1.id());
        }
        return false;
    }
}
