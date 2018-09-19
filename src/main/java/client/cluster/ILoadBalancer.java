package client.cluster;

import java.util.List;

/**
 * Copyright (C) 22018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:44:39
 */
public interface ILoadBalancer {
    public RemoteServer select(List<RemoteServer> list);
}
