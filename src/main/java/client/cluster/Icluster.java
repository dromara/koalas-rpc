package client.cluster;

import java.util.List;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:44:33
 */
public interface Icluster {

    //获取当前使用的远程服务
    public RemoteServer getUseRemote();

    //销毁长连接资源等
    public void destroy();

    //获取资源池
    public ServerObject getObjectForRemote();

}
