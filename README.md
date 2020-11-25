![logo][logopng]
<br/>
<br/>
---
一个自动注册扩展服务发现、netty长连接池的高性能轻量级RPC框架
<br/>

[![netty][nettysvg]][netty] [![zk][zksvg]][zk]  [![License][licensesvg]][license]

  * 使用zookeeper服务发现,自动注册扩展服务
  * 使用长连接TCP池,netty作为网络IO,支持全双工通信,高性能
  * 消息发送支持异步/同步,NIO
  * 自动选择符合action节点服务器,支持权重分发消息
  * 支持snappy,gzip压缩
  * 可进行二次封装开发,[远程调用][qsrpc-starter],消息路由负载均衡等等
  * 欢迎学习交流~[RPC项目技术选型及简介]

![ad][adpng]
## Maven
```
<dependency>
    <groupId>com.github.tohodog</groupId>
    <artifactId>qsrpc</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Demo
First configured [zookeeper](http://mirrors.hust.edu.cn/apache/zookeeper/)

### application.properties
```
#zookeeper
qsrpc.zk.ips=127.0.0.1:2181
qsrpc.zk.path=/qsrpc

#node server
qsrpc.node.ip=127.0.0.1
qsrpc.node.port=19980
qsrpc.node.action=user,order
qsrpc.node.weight=1
qsrpc.node.zip=snappy/gzip
```

### Node
```
    //open node server 1
    NodeInfo nodeInfo = NodeRegistry.buildNode();//read application.properties
    //sync callback
    NodeLauncher.start(nodeInfo, new MessageListener() {
        @Override
        public byte[] onMessage(Async async, byte[] message) {
        return ("Hello! node1 callback -" + new String(message)).getBytes();
        }
    });

    //open node server 2
    NodeInfo nodeInfo2 = new NodeInfo();
    nodeInfo2.setZkIps("127.0.0.1:2181");//zookeeper ip
    nodeInfo2.setZkPath("/qsrpc");//zookeeper path
    nodeInfo2.setAction("order");//node server action
    nodeInfo2.setIp("127.0.0.1");//node server ip
    nodeInfo2.setPort(8848);//nodeserver port
    nodeInfo2.setWeight(2);//request weight

    //async callback
    NodeLauncher.start(nodeInfo2, new MessageListener() {
        @Override
        public byte[] onMessage(final Async async, final byte[] message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
            async.callBack(("Hello! node2 callback -" + new String(message)).getBytes());
            }
        }).start();
        return null;
        }
    });
```
### Client
```
    //async
    for (int i = 0; i < 9; i++) {
    	//Send byte[] based on action
        RPCClientManager.getInstance().sendAsync("user", "user".getBytes(),
            new Callback<byte[]>() {
            @Override
            public void handleResult(byte[] result) {
                System.out.println("send [user] Result: " + new String(result));
            }

            @Override
            public void handleError(Throwable error) {
                error.printStackTrace();
            }
            });
    }
    System.out.println("send [user] Done");

    //sync
    for (int i = 0; i < 9; i++) {
        Thread.sleep(1000);
        byte[] msg_cb = RPCClientManager.getInstance().sendSync("order", "order".getBytes());
        System.out.println("send [order] Result: " + new String(msg_cb));
    }
    System.out.println("send [order] Done");

    //future
    CallFuture<byte[]> callFuture = RPCClientManager.getInstance().sendAsync("user", "user".getBytes());
    System.out.println("send [user] FutureResult: " + new String(callFuture.get()));
```
## Test
Run [TestConcurrent.java][testjava] (Don't open the console and 360 antivirus etc.)

|  CPU   | request  | time  |qps  |
|  ----  | ----  |----  |----  |
| i3-8100(4-core/4-thread) | 100w(8-thread) |7817ms | 127926  |
| i7-8700(6-core/12-thread) | 120w(24-thread) |4930ms | 243407  |

在4核自发自收的情况下有12万+的并发数,实际会更高 [测试截图1][testpng] [测试截图2][testpng2]

## Future
  * Support nacos...
  * AIO...
  


## QSRPC项目技术选型及简介
### 1.TCP通信
#### 1.1 连接模式:
　本项目tcp通信使用长连接+全双工通信(两边可以同时收/发消息),可以保证更大的吞吐量/更少的连接数资源占用,理论上使用一个tcp连接即可满足通信(详见pool),如果使用http/1.1协议的请求-响应模式,同一个连接在同一个时刻只有有一个消息进行传输,如果有大量请求将会阻塞或者需要开更多tcp连接来解决
#### 1.2 协议:
|TCP|长度|消息ID|协议号|加密/压缩|内容|包尾|
|:----:|:----:|:----:|:----:|:----:|:----:|:----:|
| Byte | 4 | 4 | 1 | 1(4bit+4bit)  | n | 2 |
　首先,使用长连接那就需要解决tcp粘包问题,常见的两种方式:  
 * 包头长度:优点最简单,也是最高效的,缺点是无法感知丢包,会导致后续所有包错乱
 * 特定包尾:优点能感知丢包,不影响后续包,缺点需要遍历所有字节,切不能与包内容冲突
 
　综上,本框架使用的是包头长度+特定包尾,结合了两者优点,避免了缺点,高效实用,检测到包错误会自动断开.
没有使用校检码转码等,因为需要考虑实际情况,内网里出错概率非常低,出错了也能重连,对于RPC框架追求性能来说是合适的,即使是外网,后续有需求可以增加校验加密协议
<br/>
　其次,因为支持全双工那就需要解决消息回调问题,本协议使用了一个消息ID,由客户端生成,服务端返回消息带上;由于发送和接收是非线性的,所以客户端需要维护一个回调池,以ID为key,value为此次请求的context(callback),因为是异步的,请求有可能没有响应,所以池需要有超时机制

#### 1.3 压缩/加密:
　当出现带宽不足而CPU性能有余时,压缩就派上用场了,用时间换空间。目前支持了snappy/gzip两种压缩,snappy应用于google的rpc上,具有高速压缩速度和合理的压缩率,gzip速度次于snappy,但压缩率较高,根据实际情况配置,前提必须是带宽出现瓶颈/要求,否则不需要开启压缩
<br/>　加密功能计划中(加盐位算法)
#### 1.4 IO框架:
网络IO目前是基于netty搭建的,支持nio,zero-copy等特性,由于本框架连接模式使用长连接,连接数固定且较少,所以本框架性能对于IO框架(BIO/NIO/AIO)并不是很敏感,netty对于http,iot服务这种有大量连接数的优势就很大了


### 2. Tcp pool
　前面说了一个tcp连接即可支撑通信,为啥又用pool了呢,原因有两个:1. netty工作线程对于同一个连接使用同一个线程来处理的,所以如果客户端发送大量请求时,服务端只有一个线程在处理导致性能问题,起初是想服务端再把消息分发到线程池,但后续测试发现此操作在高并发下会导致延迟增大,因为又把消息放回线程池排队了。2. 相对于一条tcp链接,使用pool会更加灵活,且连接数也很少,并没有性能影响; 本框架还基于pool实现了一个[请求-响应]的通信模式*
<br>
　客户端Pool的maxIdle(maxActive)=服务节点配置的CPU线程数*2=服务节点netty的工作线程数,pool采用FIFO先行先出的策略,可以保证在高并发下均匀的使用tcp连接,服务端就不用再次分发消息了
### 3. 服务注册发现
　分布式系统中都需要一个配置/服务中心,才能进行统一管理.本框架目前使用zookeeper(后面会支持nacos)进行服务注册,zookeeper是使用类似文件目录的结构,每个目录都可以存一个data
<br>　节点注册是使用[IP:PROT_TIME]作为目录名,data存了节点的json数据,创建模式为EPHEMERAL_SEQUENTIAL(断开后会删除该目录),这样就达到了自动监听节点上下线的效果,加入时间戳是为了解决当节点快速重启时,注册了两个目录,便于进行区分处理
<br>　客户端通过watch目录变化信息,从而获取到所有服务节点信息,同步一个副本到本地Map里(需加上读写锁),客户端就可以实现调用对应的服务了


## Log
### v1.1.1(2020-11-22)
  * Support compress
  * Optimization pool log test...

### v1.0.1(2019-09-26)
  * Support future get
  * Optimization
### v1.0.0(2019-09-19)
  * Open sourse

## Other
  * 有问题请Add [issues](https://gitee.com/sakaue/QSRPC/issues)
  * 如果项目对你有帮助的话欢迎[star][star]
<br/>
<br/>


[logopng]: https://gitee.com/sakaue/QSRPC/raw/master/logo.png
[adpng]: https://gitee.com/sakaue/QSRPC/raw/master/Architecture_diagram.jpg
[testpng]: https://gitee.com/sakaue/QSRPC/raw/master/test.png
[testjava]: https://gitee.com/sakaue/QSRPC/raw/master/src/test/java/test/TestConcurrent.java
[testpng2]: https://gitee.com/sakaue/QSRPC/raw/master/test2.png


[nettysvg]: https://img.shields.io/badge/netty-4.1.42-greed.svg
[netty]: https://github.com/netty/netty

[zksvg]: https://img.shields.io/badge/zookeeper-3.4.14-blue.svg
[zk]: https://github.com/apache/zookeeper

[licensesvg]: https://img.shields.io/badge/License-Apache--2.0-red.svg
[license]: https://gitee.com/sakaue/QSRPC/raw/master/LICENSE

[starsvg]: https://img.shields.io/github/stars/tohodog/QSRPC.svg?style=social&label=Stars
[star]: https://gitee.com/sakaue/QSRPC

[qsrpc-starter]: https://gitee.com/sakaue/QSRPC-starter
