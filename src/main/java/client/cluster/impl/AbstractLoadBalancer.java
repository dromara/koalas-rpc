package client.cluster.impl;

import client.cluster.ILoadBalancer;
import client.cluster.RemoteServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * Copyright (C) 22018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:44:08
 */
public abstract class AbstractLoadBalancer implements ILoadBalancer {

    public int getWeight(RemoteServer remoteServer){
        if(remoteServer != null && remoteServer.isEnable ()){
            return remoteServer.getWeight ();
        }
        return -1;
    }

    public RemoteServer select(List<RemoteServer> list){

        if(list ==null) return null;

        if(list.size ()==1){
            return list.get ( 0 );
        }

        List<RemoteServer> l = new ArrayList<> (  );

        for (int i = list.size ()-1; i >=0 ; i--) {
            RemoteServer r = list.get ( i );
            if(r.isEnable ()){
                l.add ( r );
            }
        }

        return doSelect(l);
    }

    public abstract RemoteServer doSelect(List<RemoteServer> list);

}
