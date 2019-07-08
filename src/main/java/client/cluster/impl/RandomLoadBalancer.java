package client.cluster.impl;

import client.cluster.RemoteServer;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年09月18日17:44:26
 */
public class RandomLoadBalancer extends  AbstractLoadBalancer {

    @Override
    public RemoteServer doSelect(List<RemoteServer> list) {
        if(list == null){
            return null;
        }

        //将list混排，这样获得的资源更公平
        Collections.shuffle (list);
        int[] array =new int[list.size ()];
        int total = 0;
        int curr=0;
        for(RemoteServer RemoteServerTemp_:list){
            total+=getWeight(RemoteServerTemp_);
            array[curr++] = total;
        }

        if(total==0){
            throw  new IllegalArgumentException ( "the remote serverList is empty!" );
        }

        Random r =new Random ();
        curr = r.nextInt (total);

        for(int c=0;c<array.length;c++){
            if(curr<array[c]){
                return list.get ( c );
            }
        }
        return null;
    }
}
