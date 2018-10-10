# koalas-rpc

#### 项目介绍
考拉RPC
    个人业务作品，提供大家交流学习，有意见请私信，欢迎拍砖。客户端采用thrift协议，服务端支持netty和thrift的TThreadedSelectorServer半同步半异步线程模型，支持动态扩容，服务上下线，（权重动态，可用性配置，页面流量统计等，2.0版本）持续为个人以及中小型公司提供可靠的RPC框架技术方案。

为什么要写这个RPC
    市面上常见的RPC框架很多，grpc，motan，dubbo等，但是随着越来越多的元素加入，复杂的架构设计等因素似使得这些框架就想spring一样，虽然号称是轻量级，但是用起来却是让我们很蹩脚，大量的配置，繁杂的API设计，其实，我们根本用不上这些东西！！！
    我也算得上是在很多个互联网企业厮杀过，见过很多很多的内部RPC框架，有些优秀的设计让我非常赞赏，有一天我突然想着，为什么不对这些设计原型进行聚合归类，自己搞一套【轻量级】RPC框架呢，碍于工作原因，一直没有时间倒腾出空，十一期间工作闲暇，说搞就搞吧，希望源码对大家对认识RPC框架起到推进的作用。东西越写越多，有各种问题欢迎随时拍砖

#### 软件架构组成元素
libthrift，spring-core，spring-context，spring-beans，log4j，slf4j，org.apache.commons，io.netty，fastjson，zookeeper

客户端
spring,apache pool,thrift等
服务端
spring,apache pool,thrift，netty等

#### 关于技术选型

1：序列化篇
  考察了很多个序列化组件，其中包括jdk原生，kryo、hessian、protostuff,thrift，json等，最终选择了Thrift，原因如下
  原生JDK序列化反序列化效率堪忧，其序列化内容太过全面kryo和hessian，json相对来说比原生JDK强一些，但是对跨语言支持一般，所以舍弃了，最终想在protobuf和Thrift协议里面选择一套框架，这俩框架很相通，支持跨语言，需要静态编译等等。但是protobuf不带RPC服务，本着提供多套服务端模式（thrift rpc，netty）的情况下，最终选择了Thrift协议。

2：IO线程模型篇
 原生socket可以模拟出简单的RPC框架，但是对于大规模并发，要求吞吐量的系统来说，也就算得上是一个demo级别的，所以BIO肯定是不考虑了，NIO的模型在序列化技术选型的时候已经说了，Thrift本身支持很多个io线程模型，同步，异步，半同步异步等（SimpleServer，TNonblockingServer，THsHaServer，TThreadedSelectorServer，TThreadPoolServer），其中吞吐量最高的肯定是半同步半异步的IO模TThreadedSelectorServer了，具体原因大家可自行google，这次不做多的阐述，选择好了模型之后，发现thrift简直就是神器一样的存在，再一想，对于服务端来说，IO模型怎么能少得了Netty啊，所以下决心也要支持Netty，但是很遗憾Netty目前没有对Thrift的序列化解析，拆包粘包的处理，但是有protobuf，和http协议的封装，怎么办，自己在netty上写对thrift的支持呗，虽然工作量大了一些，但是一想netty不就是干这个事儿的嘛- -！

3：服务发现
 支持集群的RPC框架里面，像dubbo，或者是其他三方框架，对服务发现都进行的封装，那么自研RPC的话，服务发现就要自己来写了，那么简单小巧容易上手的zookeeper肯定是首选了。

到目前为止 序列化+IO模型+高可用服务发现机制的技术选型已经落实，也就是RPC性能三大要素：序列化+IO线程模型+高可用服务发现集群。

#### 安装教程
考拉RPC确保精简，轻量的原则，只需要zk服务器进行服务发现（后续版本服务治理可能需要Datasource），对于zookeeper的各个环境安装教程请自行google，不在本安装教程内特意说明


# 使用说明

服务端和客户端都需要引入考拉RPC服务（目前还没有上传到码云，阿里云，和maven中央镜像，本地下载源码后clean install到本地仓库使用即可，2.0版本功能全面之后会统一上传）

       <?xml version="1.0" encoding="UTF-8"?>
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

#### 1：客户端使用方式

以下是最精简配置

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	   xsi:schemaLocation="http://www.springframework.org/schema/beans
	http://www.springframework.org/schema/beans/spring-beans-4.2.xsd">

	<bean id="wmCreateAccountService" class="client.proxyfactory.KoalasClientProxy" destroy-method="destroy">
		<property name="serviceInterface" value="thrift.service.WmCreateAccountService"/>
		<property name="zkPath" value="127.0.0.1:2181"/>
	</bean>

    <bean id="xxxx" class="client.proxyfactory.KoalasClientProxy" destroy-method="destroy">
		<property name="serviceInterface" value="thrift.xxxx.WmCreateAccountService"/>
		<property name="zkPath" value="127.0.0.1:2181"/>
	</bean>
</beans>


client.proxyfactory.KoalasClientProxy 为基础服务类，copy引入即可。
其中serviceInterface为thrift生成的服务类，需要全局唯一，（关于thrift服务类生成请自行google，网上很多，这里不多阐述），zkPath为zookeeper的地址，集群环境请用逗号分隔 【127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183】


![输入图片说明](https://images.gitee.com/uploads/images/2018/1010/172210_ed5d3a00_536094.png "屏幕截图.png")

在你的服务类里面对服务类进行注入就可以了，注意是xxxx.iface。


#### 2：服务端使用方式

<?xml version="1.0" encoding="UTF-8"?>

	<bean class ="server.KoalasServerPublisher" destroy-method="destroy">
		<property name="serviceInterface" value="thrift.service.WmCreateAccountService"/>
		<property name="serviceImpl" ref="wmCreateAccountServiceImpl"/>
		<property name="port" value="8001"/>
		<property name="zkpath" value="127.0.0.1:2181"/>
	</bean>

</beans>

serviceInterface和客户端一样是thrift生成的服务类。
serviceImpl是服务类实现。
port是服务类暴露端口，可以自定义设置，同一台机器上不重复即可，客户端会根据serviceInterface自动查找。
zkpath注册的zookeeper的地址。

wmCreateAccountServiceImpl简单实现类的截图如下
![输入图片说明](https://images.gitee.com/uploads/images/2018/1010/173130_78d04258_536094.png "屏幕截图.png")
实现WmCreateAccountService.Iface接口即可。


#### 3：所有参数配置说明
整理中


# 实际性能压测
8C 16G mac开发本，单机10000次请求耗时截图
![输入图片说明](https://images.gitee.com/uploads/images/2018/1010/174547_9325018d_536094.png "屏幕截图.png")

10w次请求，大约耗时12s，平均qps在8000左右，在集群环境下会有不错的性能表现


# 下版本计划
服务治理支持，数据统计（错误率，tp90，tp99等），数据大盘统计，自定义标签等




#### 联系作者 :
QQ 492926917