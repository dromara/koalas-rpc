package server.config;

import java.util.Objects;
/**
 * Copyright (C) 2018
 * All rights reserved
 * User: yulong.zhang
 * Date:2018年11月23日11:13:33
 */
public class ZookServerConfig {
    private String zkpath;
    private String service;
    private String env;
    private int port;
    private int weight;
    private String server;

    @Override
    public String toString() {
        return "ZookServerConfig{" +
                "zkpath='" + zkpath + '\'' +
                ", service='" + service + '\'' +
                ", env='" + env + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                ", server='" + server + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZookServerConfig that = (ZookServerConfig) o;
        return port == that.port &&
                weight == that.weight &&
                Objects.equals(zkpath, that.zkpath) &&
                Objects.equals(service, that.service) &&
                Objects.equals(env, that.env) &&
                Objects.equals(server, that.server);
    }

    @Override
    public int hashCode() {

        return Objects.hash(zkpath, service, env, port, weight, server);
    }

    public String getZkpath() {

        return zkpath;
    }

    public void setZkpath(String zkpath) {
        this.zkpath = zkpath;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public ZookServerConfig(String zkpath, String service, String env, int port, int weight, String server) {
        this.zkpath = zkpath;
        this.service = service;
        this.env = env;
        this.port = port;
        this.weight = weight;
        this.server = server;
    }
}
