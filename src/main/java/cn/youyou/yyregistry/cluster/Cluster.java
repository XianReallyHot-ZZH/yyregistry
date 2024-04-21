package cn.youyou.yyregistry.cluster;

import cn.youyou.yyregistry.YYRegistryConfigProperties;
import cn.youyou.yyregistry.service.YYRegistryService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 注册中心服务端的集群能力
 */
@Slf4j
public class Cluster {

    // 服务端实例端口
    @Value("${server.port}")
    String port;

    // 服务端实例ip
    String host;

    // 本地服务端实例对象
    Server MYSELF;

    // 注册中心服务端集群配置（目前只有集群地址）
    YYRegistryConfigProperties registryConfigProperties;

    public Cluster(YYRegistryConfigProperties registryConfigProperties) {
        this.registryConfigProperties = registryConfigProperties;
    }

    // 注册中心服务端集群对象
    @Getter
    private List<Server> servers;

    ServerHealth serverHealth;

    public Server self() {
        // 获取当前节点的时候，保证版本号是最新的
        MYSELF.setVersion(YYRegistryService.VERSION.get());
        return MYSELF;
    }

    public Server leader() {
        return servers.stream().filter(Server::isStatus).filter(Server::isLeader).findFirst().orElse(null);
    }

    public void init() {
        // 获取本机ip
        try {
            host = new InetUtils(new InetUtilsProperties()).findFirstNonLoopbackHostInfo().getIpAddress();
            log.info(" ===> findFirstNonLoopbackHostInfo = " + host);
        } catch (Exception e) {
            log.warn(" ===> can not find host, use 127.0.0.1 instead. " + e.getMessage());
            host = "127.0.0.1";
        }

        // 本机server对象
        MYSELF = new Server("http://" + host + ":" + port, true, false, -1);
        log.info(" ===> MYSELF server = " + MYSELF);

        // 构建集群server对象
        initServers();

        // 周期性触发集群内服务节点自身的探活、选主、数据同步
        serverHealth = new ServerHealth(this);
        serverHealth.checkServerHealth();
    }

    private void initServers() {
        // 构建集群server对象
        this.servers = new ArrayList<>();
        this.servers.add(MYSELF);
        HashSet<Server> servers = new HashSet<>();
        registryConfigProperties.getServerList().forEach(url -> {
            // 将本地的127.0.0.1，localhost都统一成ip
            if (url.contains("localhost")) {
                url = url.replace("localhost", host);
            } else if (url.contains("127.0.0.1")) {
                url = url.replace("127.0.0.1", host);
            }

            // 判断是否是本机
            if (url.equals(MYSELF.getUrl())) {
                log.info(" ===> 本机默认为集群中的一个server节点，无需显式加入集群");
            } else {
                // 不是本机，那么创建，然后添加进池子
                servers.add(new Server(url, false, false, -1));
            }
        });
        this.servers.addAll(servers);
    }

}

