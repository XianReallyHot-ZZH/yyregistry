package cn.youyou.yyregistry.cluster;

import cn.youyou.yyregistry.YYRegistryConfigProperties;
import cn.youyou.yyregistry.http.HttpInvoker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    long timeInterval = 5_000;

    public Server self() {
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

        // 周期性触发集群内服务节点自身的探活、选主、数据同步
        executor.scheduleAtFixedRate(() -> {
            try {
                // 探活、获取集群其他服务节点的信息
                updateServers();
                // 基于获取到的集群节点信息进行选主
                electLeader();
                // 基于选主的结果进行数据同步（从节点的注册信息从主节点同步过来）
                syncSnapshotFromLeader(); // 3.同步快照
            } catch (Exception e) {
                log.error(" ===> 集群自身分布式功能出现异常.", e);
            }
        }, 0, timeInterval, TimeUnit.MILLISECONDS);

    }

    /**
     * 探活集群其他节点，获取集群其他服务节点的信息，完成集群信息更新
     */
    private void updateServers() {
        // 遍历除了自己外的服务节点，进行探活，顺便更新节点信息
        servers.forEach(server -> {
            if (server.equals(MYSELF)) {
                MYSELF.setStatus(true);
                return;
            }
            // 探活
            try {
                Server serverInfo = HttpInvoker.httpGet(server.getUrl() + "/info", Server.class);
                log.info(" ===>>> health check success for " + serverInfo);
                if (serverInfo != null) {
                    server.setStatus(true);
                    server.setLeader(serverInfo.isLeader());
                    server.setVersion(serverInfo.getVersion());
                }
            } catch (Exception e) {
                log.error(" ===>>> health check failed for " + server);
                server.setStatus(false);
                server.setLeader(false);
            }
        });
    }

    /**
     * 基于当前的集群节点信息进行选主操作
     */
    private void electLeader() {
        // 过滤出leader节点
        List<Server> leaders = servers.stream().filter(Server::isStatus).filter(Server::isLeader).toList();
        if (leaders.isEmpty()) {
            // 当前集群没有leader，这是一个异常的状态，需要进行选主
            log.info(" ===>>> ^&**^&&** elect for no leader: " + servers);
            doElectLeader();
        } else if (leaders.size() > 1) {
            // 当前集群有多个leader，这是一个异常的状态，需要进行选主
            log.info("  ^&**^&&** ===>>> ^&**^&&** elect for more than one leader: " + servers);
            doElectLeader();
        } else {
            // 当前集群只有一个leader，正常
            log.info(" ===>>> no need election for leader: " + leaders.get(0));
        }
    }

    private void doElectLeader() {
        // 常用方式
        // 1.各种节点自己选，算法保证大家选的是同一个
        // 2.外部有一个分布式锁，谁拿到锁，谁是主
        // 3.分布式一致性算法，比如paxos,raft，，很复杂

        Server candidate = null;
        // 在集群活着的所有节点中选择hashcode最小的节点作为leader
        for (Server server : servers) {
            server.setLeader(false);
            if (server.isStatus()) {
                if (candidate == null) {
                    candidate = server;
                } else if (server.hashCode() < candidate.hashCode()) {
                    candidate = server;
                }
            }
        }
        if (candidate != null) {
            candidate.setLeader(true);
            log.info(" ===>>> elect for leader: " + candidate);
        } else {
            log.error(" ===>>> elect failed for no leaders: " + servers);
        }
    }

    /**
     * 基于选主的结果进行数据同步
     * （从节点的注册信息从主节点同步过来，以主节点的注册信息为准，
     * 只有主节点对外有写的能力，总节点对外只有读的能力）
     */
    private void syncSnapshotFromLeader() {
    }

}

