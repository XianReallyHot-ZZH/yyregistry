package cn.youyou.yyregistry.cluster;

import cn.youyou.yyregistry.http.HttpInvoker;
import cn.youyou.yyregistry.service.YYRegistryService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerHealth {

    final Cluster cluster;

    final Election election;

    public ServerHealth(Cluster cluster) {
        this.cluster = cluster;
        election = new Election();
    }

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    long timeInterval = 5_000;

    public void checkServerHealth() {
        // 周期性触发集群内服务节点自身的探活、选主、数据同步
        executor.scheduleAtFixedRate(() -> {
            try {
                // 探活、获取集群其他服务节点的信息
                updateServers();
                // 基于获取到的集群节点信息进行选主
                doElect();
                // 基于选主的结果进行数据同步（从节点的注册信息从主节点同步过来）
                syncSnapshotFromLeader(); // 3.同步快照
            } catch (Exception e) {
                log.error(" ===> 集群自身分布式功能出现异常.", e);
            }
        }, 0, timeInterval, TimeUnit.MILLISECONDS);
    }

    private void doElect() {
        election.electLeader(cluster.getServers());
    }

    /**
     * 探活集群其他节点，获取集群其他服务节点的信息，完成集群信息更新
     */
    private void updateServers() {
        // 遍历除了自己外的服务节点，进行探活，顺便更新节点信息
        List<Server> servers = cluster.getServers();
        servers.stream().parallel().forEach(server -> {
            if (server.equals(cluster.self())) {
                cluster.self().setStatus(true);
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
     * 基于选主的结果进行数据同步
     * （从节点的注册信息从主节点同步过来，以主节点的注册信息为准，
     * 只有主节点对外有写的能力，总节点对外只有读的能力）
     */
    private void syncSnapshotFromLeader() {
        Server self = cluster.self();
        Server leader = cluster.leader();
        log.debug(" ===>>> leader version: " + leader.getVersion()
                + ", my version: " + self.getVersion());
        // 本地节点不是leader，并且版本号落后于leader，那么进行数据同步
        if (!self.isLeader() && self.getVersion() < leader.getVersion()) {
            log.debug(" ===>>> sync snapshot from leader: " + leader);
            Snapshot snapshot = HttpInvoker.httpGet(leader.getUrl() + "/snapshot", Snapshot.class);
            log.debug(" ===>>> sync and restore snapshot: " + snapshot);
            YYRegistryService.snapshotRestore(snapshot);
        }
    }

}
