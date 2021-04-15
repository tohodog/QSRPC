package org.song.qsrpc;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/12/7
 * rpc配置
 */
public class RPCConfig {

    /**
     * 是否打印请求日记
     */
    private boolean printLog;

    /**
     * zookeeper 服务地址多个,隔开
     */
    private String zkIps;
    /**
     * zookeeper 创建路径
     */
    private String zkPath;
    /**
     * zookeeper 用户名
     */
    private String zkUserName;
    /**
     * zookeeper 密码
     */
    private String zkPassword;

    /**
     * nacos 服务地址多个,隔开
     */
    private String nacosAddr;
    /**
     * nacos 服务发现名
     */
    private String nacosServiceName;

    /**
     * 项目名
     */
    private String nodeName;


    /**
     * 客户端 请求超时
     */
    private int clientTimeout;


    /**
     * 节点 IP
     */
    private String nodeIp;
    /**
     * 节点 端口
     */
    private int nodePort;
    /**
     * 节点 提供处理的业务名,客户端是根据这个选择对应的服务节点,如 user,order
     */
    private String[] nodeAction;
    /**
     * 节点 请求权重
     */
    private int nodeWeight;
    /**
     * 节点 压缩snappy gzip
     */
    private String nodeZip;
    /**
     * 节点 线程数
     */
    private int nodeThread;
    /**
     * 节点 限制接收消息最大长度
     */
    private int nodeMaxLen;
    /**
     * 节点 服务端接收消息是否再分发进线程池(解决消息都在同一个tcp发来导致只有一个线程在处理),如果出现服务端吃不满cpu可尝试配置true
     */
    private boolean nodeRedistribute;


    public RPCConfig() {

    }

    public boolean isPrintLog() {
        return printLog;
    }

    public void setPrintLog(boolean printLog) {
        this.printLog = printLog;
    }

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

    public String getZkUserName() {
        return zkUserName;
    }

    public void setZkUserName(String zkUserName) {
        this.zkUserName = zkUserName;
    }

    public String getNacosAddr() {
        return nacosAddr;
    }

    public void setNacosAddr(String nacosAddr) {
        this.nacosAddr = nacosAddr;
    }

    public String getNacosServiceName() {
        return nacosServiceName;
    }

    public void setNacosServiceName(String nacosServiceName) {
        this.nacosServiceName = nacosServiceName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getZkPassword() {
        return zkPassword;
    }

    public void setZkPassword(String zkPassword) {
        this.zkPassword = zkPassword;
    }

    public int getClientTimeout() {
        return clientTimeout;
    }

    public void setClientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String[] getNodeAction() {
        return nodeAction;
    }

    public void setNodeAction(String[] nodeAction) {
        this.nodeAction = nodeAction;
    }

    public int getNodeWeight() {
        return nodeWeight;
    }

    public void setNodeWeight(int nodeWeight) {
        this.nodeWeight = nodeWeight;
    }

    public String getNodeZip() {
        return nodeZip;
    }

    public void setNodeZip(String nodeZip) {
        this.nodeZip = nodeZip;
    }

    public int getNodeThread() {
        return nodeThread;
    }

    public void setNodeThread(int nodeThread) {
        this.nodeThread = nodeThread;
    }

    public int getNodeMaxLen() {
        return nodeMaxLen;
    }

    public void setNodeMaxLen(int nodeMaxLen) {
        this.nodeMaxLen = nodeMaxLen;
    }

    public boolean isNodeRedistribute() {
        return nodeRedistribute;
    }

    public void setNodeRedistribute(boolean nodeRedistribute) {
        this.nodeRedistribute = nodeRedistribute;
    }
}
