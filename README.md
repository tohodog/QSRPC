![logo][logopng]
<br/>
<br/>
---
<br/>

[![netty][nettysvg]][netty] [![zk][zksvg]][zk]  [![License][licensesvg]][license]

  * 使用zookeeper服务发现,自动注册扩展服务
  * 使用长连接TCP池,netty作为网络IO,支持全双工通信,高性能
  * 消息发送支持异步/同步,NIO
  * 自动选择符合action节点服务器,支持权重分发消息
  * 支持snappy,gzip压缩
  * 可进行二次封装开发,远程调用(计划中),消息路由负载均衡等等
  * 欢迎学习交流~

![ad][adpng]
## Maven
```
	<dependency>
	    <groupId>com.gitee.sakaue</groupId>
	    <artifactId>QSRPC</artifactId>
	    <version>1.1.0</version>
	</dependency>

	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
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
Run [TestConcurrent.java]([testjava]) (Don't open the console)

|  CPU   | request  | time  |qps  |
|  ----  | ----  |----  |----  |
| i3-8100(4-core) | 100w(8-thread) |7817ms | 127926  |

4核自发自收的情况下有12万+的并发数,实际会更高 [Test截图][testpng]

## Future
  * Support eureka nacos...
  
## Log
### v1.1.0(2020-11-11)
  * Support compress
  * Optimization
### v1.0.1(2019-09-26)
  * Support future get
  * Optimization
### v1.0.0(2019-09-19)
  * Open sourse

## Other
  * 有问题请Add [issues](https://gitee.com/sakaue/QSRPC/issues)
  * 如果项目对你有帮助的话欢迎[star][star]
  
[logopng]: https://gitee.com/sakaue/QSRPC/raw/master/logo.png
[adpng]: https://gitee.com/sakaue/QSRPC/raw/master/Architecture_diagram.jpg
[testpng]: https://gitee.com/sakaue/QSRPC/raw/master/test.png
[testjava]: https://gitee.com/sakaue/QSRPC/raw/master/src/test/java/TestConcurrent.java


[nettysvg]: https://img.shields.io/badge/netty-4.1.42-greed.svg
[netty]: https://github.com/netty/netty

[zksvg]: https://img.shields.io/badge/zookeeper-3.4.14-blue.svg
[zk]: https://github.com/apache/zookeeper

[licensesvg]: https://img.shields.io/badge/License-Apache--2.0-red.svg
[license]: https://gitee.com/sakaue/QSRPC/raw/master/LICENSE

[starsvg]: https://img.shields.io/github/stars/tohodog/QSRPC.svg?style=social&label=Stars
[star]: https://gitee.com/sakaue/QSRPC
