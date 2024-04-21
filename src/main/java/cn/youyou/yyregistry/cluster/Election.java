package cn.youyou.yyregistry.cluster;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 集群节点选主功能实现类
 */
@Slf4j
public class Election {

    /**
     * 基于当前的集群节点信息进行选主操作
     */
    public void electLeader(List<Server> servers) {
        // 过滤出leader节点
        List<Server> leaders = servers.stream().filter(Server::isStatus).filter(Server::isLeader).toList();
        if (leaders.isEmpty()) {
            // 当前集群没有leader，这是一个异常的状态，需要进行选主
            log.info(" ===>>> ^&**^&&** elect for no leader: " + servers);
            doElectLeader(servers);
        } else if (leaders.size() > 1) {
            // 当前集群有多个leader，这是一个异常的状态，需要进行选主
            log.info("  ^&**^&&** ===>>> ^&**^&&** elect for more than one leader: " + servers);
            doElectLeader(servers);
        } else {
            // 当前集群只有一个leader，正常
            log.info(" ===>>> no need election for leader: " + leaders.get(0));
        }
    }

    private void doElectLeader(List<Server> servers) {
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
}
