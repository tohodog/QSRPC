![logo][logopng]
<br/>
<br/>
---
<br/>

[![netty][nettysvg]][netty] [![zk][zksvg]][zk]  [![License][licensesvg]][license]

  * 使用zookeeper服务发现
  * 使用长连接TCP池,netty作为网络IO,支持全双工通信
  * 消息发送支持异步/同步,NIO
  * 自动选择符合action节点服务器,支持权重分发消息
  * 欢迎学习交流~

![ad][adpng]

## Maven
```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<dependency>
	    <groupId>com.github.tohodog</groupId>
	    <artifactId>QSRPC</artifactId>
	    <version>1.0.0</version>
	</dependency>
``` 

## Demo
First configured [zookeeper](http://mirrors.hust.edu.cn/apache/zookeeper/)

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
### application.properties
```
qsrpc.zk.ips=127.0.0.1:2181
qsrpc.zk.path=/qsrpc

qsrpc.node.ip=127.0.0.1
qsrpc.node.port=19980
qsrpc.node.action=user,order
qsrpc.node.weight=1
```
### Client
```
    //async
    for (int i = 0; i < 9; i++) {
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

```

 

## Log
### v1.0.0(2019-09-19)
  * Open sourse

## Other
  * 有问题请Add [issues](https://github.com/tohodog/QSRPC/issues)
  * 如果项目对你有帮助的话欢迎[![star][starsvg]][star]
  
[logopng]: https://raw.githubusercontent.com/tohodog/QSRPC/master/logo.png
[adpng]: https://raw.githubusercontent.com/tohodog/QSRPC/master/Architecture_diagram.png

[nettysvg]: https://img.shields.io/badge/netty-4.1.13-greed.svg
[netty]: https://github.com/netty/netty

[zksvg]: https://img.shields.io/badge/zookeeper-3.4.10-blue.svg
[zk]: https://github.com/apache/zookeeper

[licensesvg]: https://img.shields.io/badge/License-Apache--2.0-red.svg
[license]: https://github.com/tohodog/QSRPC/blob/master/LICENSE

[starsvg]: https://img.shields.io/github/stars/tohodog/QSRPC.svg?style=social&label=Stars
[star]: https://github.com/tohodog/QSRPC
