# koalas-rpc

#### 项目介绍
koalas-RPC
    个人作品，提供大家交流学习，有意见请私信，欢迎拍砖。客户端采用thrift协议，服务端支持netty和thrift的TThreadedSelectorServer半同步半异步线程模型，支持动态扩容，服务上下线，权重动态，可用性配置，页面流量统计等，2.0版本，QPS统计，TP90,TP99,TP95等丰富可视化数据，持续为个人以及中小型公司提供可靠的RPC框架技术方案。

#### 为什么要写这个RPC
市面上常见的RPC框架很多，grpc，motan，dubbo等，但是随着越来越多的元素加入，复杂的架构设计等因素似使得这些框架和spring一样，虽然号称是轻量级，但是用起来却是让我们很蹩脚，大量的配置，繁杂的API设计，其实，我们根本用不上这些东西！！！
我也算得上是在很多个互联网企业厮杀过，见过很多很多的内部RPC框架，有些优秀的设计让我非常赞赏，有一天我突然想着，为什么不对这些设计原型进行聚合归类，自己搞一套【轻量级】RPC框架呢，碍于工作原因，一直没有时间倒腾出空，十一期间工作闲暇，说搞就搞吧，希望源码对大家对认识RPC框架起到推进的作用。东西越写越多，有各种问题欢迎随时拍砖

#### 为什么叫koalas
 树袋熊英文翻译，希望考拉RPC给那些不太喜欢动手自己去造轮子的人提供可靠的RPC使用环境

#### 软件架构组成元素
thrift 0.8.0
spring-core，spring-context，spring-beans(4.2.5 理论上支持spring2.5以上版本，可替换成自己项目版本)
log4j，slf4j
org.apache.commons(v2.0+)
io.netty(v4.0+)
fastJson
zookeeper3.4.6
点评cat（V3.0.0+ 做数据大盘统计上报等使用，可不配置服务端，则失去数据大盘功能，但是RPC功能不受影响）

#### 关于技术选型

1：序列化篇
  考察了很多个序列化组件，其中包括jdk原生，kryo、hessian、protoStuff,thrift，json等，最终选择了Thrift，原因如下
  原生JDK序列化反序列化效率堪忧，其序列化内容太过全面kryo和hessian，json相对来说比原生JDK强一些，但是对跨语言支持一般，所以舍弃了，最终想在protoBuf和Thrift协议里面选择一套框架，这俩框架很相通，支持跨语言，需要静态编译等等。但是protoBuf不带RPC服务，本着提供多套服务端模式（thrift rpc，netty）的情况下，最终选择了Thrift协议。

2：IO线程模型篇
 原生socket可以模拟出简单的RPC框架，但是对于大规模并发，要求吞吐量的系统来说，也就算得上是一个demo级别的，所以BIO肯定是不考虑了，NIO的模型在序列化技术选型的时候已经说了，Thrift本身支持很多个io线程模型，同步，异步，半同步异步等（SimpleServer，TNonblockingServer，THsHaServer，TThreadedSelectorServer，TThreadPoolServer），其中吞吐量最高的肯定是半同步半异步的IO模TThreadedSelectorServer了，具体原因大家可自行google，这次不做多的阐述，选择好了模型之后，发现thrift简直就是神器一样的存在，再一想，对于服务端来说，IO模型怎么能少得了Netty啊，所以下决心也要支持Netty，但是很遗憾Netty目前没有对Thrift的序列化解析，拆包粘包的处理，但是有protoBuf，和http协议的封装，怎么办，自己在netty上写对thrift的支持呗，虽然工作量大了一些，但是一想netty不就是干这个事儿的嘛- -！

3：服务发现
 支持集群的RPC框架里面，像dubbo，或者是其他三方框架，对服务发现都进行的封装，那么自研RPC的话，服务发现就要自己来写了，那么简单小巧容易上手的zookeeper肯定是首选了。

到目前为止 序列化+IO模型+高可用服务发现机制的技术选型已经落实，也就是RPC性能三大要素：序列化+IO线程模型+高可用服务发现集群。

#### 安装教程
考拉RPC确保精简，轻量的原则，只需要zk服务器进行服务发现（后续版本服务治理可能需要Datasource），对于zookeeper的各个环境安装教程请自行google，不在本安装教程内特意说明
如果需要cat的数据大盘功能，想更方便的查看服务的调用情况，需要安装cat服务，至于cat的安装就更简单了，就是war包扔在tomcat里面运行，然后配置一些参数即可，当然你也可以不接入cat，单独的作为RPC框架来使用。
CAT接入参考：https://github.com/dianping/cat
# 使用说明

服务端和客户端都需要引入考拉RPC服务（目前还没有上传到码云，阿里云，和maven中央镜像，本地下载源码后clean install到本地仓库使用即可，2.0版本功能全面之后会统一上传），当然并不希望所有的依赖都exclusion，知识不希望覆盖掉用户其他版本的依赖，这个地方需要按需处理。

       <dependency>
            <groupId>koalas.rpc</groupId>
            <artifactId>com.Koalas.rpc</artifactId>
            <version>Koalas-1.0-SNAPSHOT</version>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>



首先需要编写自己的thrift idl文件了，这里多说一句，在群里的小伙伴曾经说过idl文件编写不熟悉，有可能出错
这里顺带说一嘴，thrift的ldl文件和写java的请求体和service几乎没有任何区别，熟能生巧，上手之后非常简单
这里推荐几篇thrift的文章，有兴趣可以看一看
https://blog.csdn.net/lk10207160511/article/details/50450541，
https://blog.csdn.net/hrn1216/article/details/51306395
下面截图为测试的thrift文件

```
namespace java thrift.service

include 'WmCreateAccountRequest.thrift'
include 'WmCreateAccountRespone.thrift'

service WmCreateAccountService {
      WmCreateAccountRespone.WmCreateAccountRespone getRPC(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest1(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest2(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest3(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest4(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest5(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest6(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest7(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest8(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest9(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest10(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest11(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
      WmCreateAccountRespone.WmCreateAccountRespone koaloasTest12(1:WmCreateAccountRequest.WmCreateAccountRequest wmCreateAccountRequest);
}
```

```
namespace java thrift.domain
/**
* 测试类
**/
struct WmCreateAccountRequest {

    1:i32 source,

    2:i32 accountType,

    3:i64 partnerId,

    4:i32 partnerType,

    5:string partnerName,

    6:i32 poiFlag,
}

```

```
namespace java thrift.domain
/**
* 测试类
**/
struct WmCreateAccountRespone {
    1:i32 code,
    2:string message,
}

```

#### 1：客户端使用方式

#### xml
以下是最精简配置 zkPath为zookeeper的地址，集群环境请用逗号分隔 【127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183】

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:koalas="http://www.koalas.com/schema/ch"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
	                       http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
                           http://www.koalas.com/schema/ch
                           http://www.koalas.com/schema/ch.xsd">

	<koalas:client id="wmCreateAccountService1"
				   serviceInterface="thrift.service.WmCreateAccountService"
				   zkPath="127.0.0.1:2181"
				   async="false"/>

</beans>

```

package thrift.service;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;

@Service("testService")
public class TestService {

    @Autowired
    WmCreateAccountService.Iface wmCreateAccountService;

    public void getRemoteRpc() throws TException {

        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.getRPC (  request);
        System.out.println (respone);
     }
}
```

在你的服务类里面对服务类进行注入就可以了，注意是xxxx.iface。

#### 异步

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
	http://www.springframework.org/schema/beans/spring-beans-4.2.xsd">

	<koalas:client id="wmCreateAccountService2"
		       serviceInterface="thrift.service.WmCreateAccountService"
	               zkPath="127.0.0.1:2181"
		       async="true"/>
</beans>

```
package thrift.service;

import client.async.KoalasAsyncCallBack;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
@Service("testService")
public class TestService2 {
    @Autowired
    WmCreateAccountService.AsyncIface wmCreateAccountService;
    public void getRemoteRpc() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.getRPC_call> 
        koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.getRPC ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

}
```
在你的服务类里面对服务类进行注入就可以了，注意是xxxx.AsyncIface。

KoalasAsyncCallBack为我为大家写的统一callback方法，支持future接口，并且支持future.get ()同步获取和future.get(long timeout, TimeUnit unit)超时获取两种方式，推荐大家使用。或者你可以自定义你自己的AsyncMethodCallback，具体实现方法参照Thrift官网。
值得说明的是 KoalasAsyncCallBack泛型类型一共有两个参数，第一个参数是方法返回类型，第二个是thrift自动生成xxxxxx_call，和原生callback接口一致


#### 注解

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xmlns:koalas="http://www.koalas.com/schema/ch"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
	                       http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
                           http://www.koalas.com/schema/ch
                           http://www.koalas.com/schema/ch.xsd">

	<koalas:annotation package="thrift.annotation.client.impl"/>
</beans>
```

同步
```
@Service("testServiceSync")
public class TestServiceSync {

    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.Iface wmCreateAccountService;

    public void getRemoteRpc() throws TException {
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是注解实现的" );
        request.setPoiFlag ( 1 );
        WmCreateAccountRespone respone = wmCreateAccountService.getRPC (  request);
        System.out.println (respone);
     }

}
```
异步


```
@Service("testServiceAsync")
public class TestServiceAsync {
    @KoalasClient(zkPath = "127.0.0.1:2181",readTimeout = 5000*1000)
    WmCreateAccountService.AsyncIface wmCreateAccountService;
    public void getRemoteRpc() throws TException{
        KoalasAsyncCallBack<WmCreateAccountRespone, WmCreateAccountService.AsyncClient.getRPC_call> koalasAsyncCallBack = new KoalasAsyncCallBack<> ();
        WmCreateAccountRequest request= new WmCreateAccountRequest (  );
        //request.setSource ( 10 );
        request.setAccountType ( 1 );
        request.setPartnerId ( 1 );
        request.setPartnerType ( 1 );
        request.setPartnerName ( "你好啊-我是注解实现的" );
        request.setPoiFlag ( 1 );
        wmCreateAccountService.getRPC ( request ,koalasAsyncCallBack);
        Future<WmCreateAccountRespone> future= koalasAsyncCallBack.getFuture ();
        try {
            System.out.println (future.get ());
        } catch (InterruptedException e) {
            e.printStackTrace ();
        } catch (ExecutionException e) {
            e.printStackTrace ();
        }
    }

}
```



#### 2：服务端使用方式

#### xml
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:koalas="http://www.koalas.com/schema/ch"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
	   http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
	   http://www.springframework.org/schema/context
	   http://www.springframework.org/schema/context/spring-context-4.2.xsd
	   http://www.koalas.com/schema/ch
	   http://www.koalas.com/schema/ch.xsd">

    <!-- 默认扫描的包路径 -->
    <context:component-scan base-package="thrift.xml.server" use-default-filters="false">
        <context:include-filter type="annotation" expression="org.springframework.stereotype.Service"/>
        <context:include-filter type="annotation" expression="org.springframework.stereotype.Component"/>
    </context:component-scan>

    <koalas:server id="WmCreateAccountService1"
                   serviceInterface="thrift.service.WmCreateAccountService"
                   serviceImpl="wmCreateAccountServiceImpl"
                   port="8001"
                   serverType="thrift"
                   zkpath="127.0.0.1:2181"/>
</beans>
```



serviceInterface和客户端一样是thrift生成的服务类。
serviceImpl是服务类实现。
port是服务类暴露端口，可以自定义设置，同一台机器上不重复即可，客户端会根据serviceInterface自动查找。
zkpath注册的zookeeper的地址。

wmCreateAccountServiceImpl简单实现类的截图如下
![输入图片说明](https://images.gitee.com/uploads/images/2018/1010/173130_78d04258_536094.png "屏幕截图.png")
实现WmCreateAccountService.Iface接口即可。

#### 注解

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:koalas="http://www.koalas.com/schema/ch"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
	   http://www.springframework.org/schema/beans/spring-beans-4.2.xsd
	   http://www.koalas.com/schema/ch
	   http://www.koalas.com/schema/ch.xsd">
    <koalas:annotation package="thrift.annotation.server.impl"/>
</beans>
```
其中package为扫描的路径多个路径用,分隔。为空时默认为spring的扫描路径


```
package thrift.annotation.server.impl;

import annotation.KoalasServer;
import org.apache.thrift.TException;
import thrift.domain.WmCreateAccountRequest;
import thrift.domain.WmCreateAccountRespone;
import thrift.service.WmCreateAccountService;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@KoalasServer ( port = 8801,zkpath="127.0.0.1:2181")
public class WmCreateAccountServiceNettyImpl implements WmCreateAccountService.Iface {
    private AtomicInteger atomicInteger = new AtomicInteger ( 0 );
    @Override
    public WmCreateAccountRespone getRPC(WmCreateAccountRequest wmCreateAccountRequest) throws TException {
        WmCreateAccountRespone wmCreateAccountRespone = new WmCreateAccountRespone ();
        wmCreateAccountRespone.setCode ( 1 );
        wmCreateAccountRespone.setMessage ( "你好啊" );
        if(new Random (  ).nextInt ( 5 )>100){
            try {
                Thread.sleep ( 5000 );
            } catch (InterruptedException e) {
                e.printStackTrace ();
            }
        }
        System.out.println ( "getRPC  start ...." + wmCreateAccountRequest + "------" + atomicInteger.incrementAndGet () );

        return wmCreateAccountRespone;
    }
}
```

#### 3：所有参数配置说明
![输入图片说明](https://images.gitee.com/uploads/images/2018/1203/175654_a020880b_536094.png "屏幕截图.png")

#### 4：客户端服务端RSA双向加密
源码中utils.KoalasRsaUtil的main方法已经为大家写好生成私钥和公钥的代码，执行即可
![输入图片说明](https://images.gitee.com/uploads/images/2018/1126/150109_89b08514_536094.png "屏幕截图.png")
自动生成4个很长的字符串
将前两个字符串放进client中，后面两个字符串放进server中，依次对应privateKey和publicKey，按照1，2，3，4傻瓜式复制即可:如图
![输入图片说明](https://images.gitee.com/uploads/images/2018/1126/150530_82c8bb6c_536094.png "屏幕截图.png")
![输入图片说明](https://images.gitee.com/uploads/images/2018/1126/150559_56305a69_536094.png "屏幕截图.png")

当其中一方的RSA秘钥无法对应，请求会报错。

![输入图片说明](https://images.gitee.com/uploads/images/2018/1126/150900_491f3ec4_536094.png "屏幕截图.png")
此时客户端会返回null
RSA对称加密适合给三方系统进行调用,对称加密会影响传输性能。

# 实际性能压测
8C 16G mac开发本，单机10000次请求耗时截图
![输入图片说明](https://images.gitee.com/uploads/images/2018/1010/174547_9325018d_536094.png "屏幕截图.png")

10w次请求，大约耗时12s，平均qps在8000左右，在集群环境下会有不错的性能表现

# 下版本计划
服务治理支持，数据统计（错误率，tp90，tp99等），数据大盘统计，自定义标签(已支持-更新于20181203)等

# 代码下载后如何测试
作者已经将测试类给大家写好，下载源码后clean install(这个应该都会吧，maven仓库作者用的是阿里云的maven私服)
![输入图片说明](https://images.gitee.com/uploads/images/2018/1220/104433_cfa23d61_536094.png "屏幕截图.png")
xml方式使用者直接运行上面的1和2即可，先开启服务端再开启客户端，当然如果您在多台机器上开启服务端，那么就自动负载均衡了。
注解方式使用者运行上面的3和4即可，先开启服务端在开启客户端，即可

注意测试的时候需要安装zookeeper服务,如果不想通过zk做服务发现，那么客户端可以进行直连
指定的server列表，逗号分隔，#分隔权重,格式192.168.3.253:6666#10,192.168.3.253:6667#10
详情见参数配置列表，但是这种办法作者是不推荐的，在生产环境下没有心跳和动态上下线功能。

#### 联系作者 :
高级java QQ群：825199617
博客地址:https://www.cnblogs.com/zyl2016/
