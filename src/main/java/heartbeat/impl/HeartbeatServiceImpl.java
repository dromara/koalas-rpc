package heartbeat.impl;

import heartbeat.request.HeartBeat;
import heartbeat.service.HeartbeatService;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.IPUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年12月20日11:22:19
 */
public class HeartbeatServiceImpl implements HeartbeatService.Iface {
    private final static Logger logger = LoggerFactory.getLogger ( HeartbeatServiceImpl.class );

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
    @Override
    public HeartBeat getHeartBeat(HeartBeat heartBeat) throws TException {
        logger.info ( "HeartBeat info :{}" ,heartBeat );
        HeartBeat heartBeatRespone = new HeartBeat();
        heartBeatRespone.setIp ( IPUtil.getIpV4 () );
        heartBeatRespone.setDate ( sdf.format ( new Date (  ) ) );
        return heartBeatRespone;
    }
}
