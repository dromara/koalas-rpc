package server.config;

import java.util.Objects;

public class ZookServerConfig {
    private String zkpath;
    private String service;
    private String env;
    private int port;
    private int weight;

    public ZookServerConfig(String zkpath, String service, String env, int port, int weight) {
        this.zkpath = zkpath;
        this.service = service;
        this.env = env;
        this.port = port;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "ZookServerConfig{" +
                "zkpath='" + zkpath + '\'' +
                ", service='" + service + '\'' +
                ", env='" + env + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                '}';
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
}
