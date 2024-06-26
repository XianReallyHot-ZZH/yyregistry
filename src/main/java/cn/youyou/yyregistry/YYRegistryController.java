package cn.youyou.yyregistry;

import cn.youyou.yyregistry.cluster.Cluster;
import cn.youyou.yyregistry.cluster.Server;
import cn.youyou.yyregistry.cluster.Snapshot;
import cn.youyou.yyregistry.model.InstanceMeta;
import cn.youyou.yyregistry.service.RegistryService;
import cn.youyou.yyregistry.service.YYRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class YYRegistryController {

    @Autowired
    RegistryService registryService;

    @Autowired
    Cluster cluster;

    @RequestMapping("/reg")
    public InstanceMeta register(@RequestParam String service, @RequestBody InstanceMeta instance) {
        log.info(" ===> register {} @ {}", service, instance);
        checkLeader();
        return registryService.register(service, instance);
    }

    /**
     * 具有写功能的操作，都要进行 leader 检查，只有leader节点才具备写的权限
     */
    private void checkLeader() {
        if(!cluster.self().isLeader()) {
            throw new RuntimeException("current server is not a leader, the leader is " + cluster.leader().getUrl());
        }
    }


    @RequestMapping("/unreg")
    public InstanceMeta unregister(@RequestParam String service, @RequestBody InstanceMeta instance) {
        log.info(" ===> unregister {} @ {}", service, instance);
        checkLeader();
        return registryService.unregister(service, instance);
    }

    @RequestMapping("/findAll")
    public List<InstanceMeta> findAllInstances(@RequestParam String service) {
        log.info(" ===> findAllInstances {}", service);
        return registryService.getAllInstances(service);
    }

    @RequestMapping("/renew")
    public Long renew(@RequestParam String service, @RequestBody InstanceMeta instance) {
        log.info(" ===> renew {} @ {}", service, instance);
        checkLeader();
        return registryService.renew(instance, service);
    }

    @RequestMapping("/renews")
    public Long renews(@RequestParam String services, @RequestBody InstanceMeta instance) {
        log.info(" ===> renews {} @ {}", services, instance);
        checkLeader();
        return registryService.renew(instance, services.split(","));
    }

    @RequestMapping("/version")
    public Long version(@RequestParam String service) {
        log.info(" ===> version {}", service);
        return registryService.version(service);
    }

    @RequestMapping("/versions")
    public Map<String, Long> versions(@RequestParam String services) {
        log.info(" ===> versions {}", services);
        return registryService.versions(services.split(","));
    }

    @RequestMapping("/info")
    public Server serverInfo() {
        log.info(" ===> info: {}", cluster.self());
        return cluster.self();
    }

    @RequestMapping("/cluster")
    public List<Server> cluster() {
        log.info(" ===> cluster servers: {}", cluster.getServers());
        return cluster.getServers();
    }

    @RequestMapping("/leader")
    public Server leader() {
        log.info(" ===> leader: {}", cluster.leader());
        return cluster.leader();
    }

    // 测试用的临时接口
    @RequestMapping("/setLeader")
    public Server setLeader() {
        cluster.self().setLeader(true);
        log.info(" ===> leader: {}", cluster.self());
        return cluster.self();
    }

    /**
     * 返回当前节点的注册信息快照
     * @return
     */
    @RequestMapping("/snapshot")
    public Snapshot snapshot() {
        return YYRegistryService.snapshot();
    }

}
