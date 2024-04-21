package cn.youyou.yyregistry.service;

import cn.youyou.yyregistry.cluster.Snapshot;
import cn.youyou.yyregistry.model.InstanceMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


@Slf4j
public class YYRegistryService implements RegistryService {

    // 服务的注册实例信息
    final static MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>();

    // 服务的版本号，用版本号来表达服务发生了变化，比如实例数
    final static Map<String, Long> VERSIONS = new ConcurrentHashMap<>();

    // 服务@实例的心跳
    public final static Map<String, Long> TIMESTAMPS = new ConcurrentHashMap<>();

    // 服务版本号发号器
    public final static AtomicLong VERSION = new AtomicLong(0);


    @Override
    public synchronized InstanceMeta register(String service, InstanceMeta instance) {
        List<InstanceMeta> instanceMetas = REGISTRY.get(service);
        if (instanceMetas != null && !instanceMetas.isEmpty()) {
            for (InstanceMeta instanceMeta : instanceMetas) {
                if (instanceMeta.equals(instance)) {
                    log.info(" ====> instance {} already registered", instance.toUrl());
                    instanceMeta.setStatus(true);
                    return instanceMeta;
                }
            }
        }
        log.info(" ====> register instance {}", instance.toUrl());
        REGISTRY.add(service, instance);
        instance.setStatus(true);
        renew(instance, service);
        VERSIONS.put(service, VERSION.incrementAndGet());
        return instance;
    }

    @Override
    public synchronized InstanceMeta unregister(String service, InstanceMeta instance) {
        List<InstanceMeta> instanceMetas = REGISTRY.get(service);
        if (instanceMetas == null || instanceMetas.isEmpty()) {
            return null;
        }
        log.info(" ====> unregister instance {}", instance.toUrl());
        instanceMetas.removeIf(instanceMeta -> instanceMeta.equals(instance));
        instance.setStatus(false);
        renew(instance, service);
        // 注销也需要对服务的版本进行增加，因为实例数发生了变化
        VERSIONS.put(service, VERSION.incrementAndGet());
        return instance;
    }

    @Override
    public List<InstanceMeta> getAllInstances(String service) {
        return REGISTRY.get(service);
    }

    @Override
    public synchronized Long renew(InstanceMeta instance, String... services) {
        long now = System.currentTimeMillis();
        for (String service : services) {
            TIMESTAMPS.put(service + "@" + instance.toUrl(), now);
        }
        return now;
    }

    @Override
    public Long version(String service) {
        return VERSIONS.get(service);
    }

    @Override
    public Map<String, Long> versions(String... services) {
        return Arrays.stream(services).collect(Collectors.toMap(k -> k, VERSIONS::get, (a, b) -> b));
    }

    /**
     * 将当前节点的注册信息打成快照
     * 注意：这里要保证原子性，即注册的信息的快照过程要保证数据的一致性，要在一个原子操作中
     * @return
     */
    public static synchronized Snapshot snapshot() {
        LinkedMultiValueMap<String, InstanceMeta> registry = new LinkedMultiValueMap<>(REGISTRY);
        Map<String, Long> versions = new HashMap<>(VERSIONS);
        Map<String, Long> timestamps = new HashMap<>(TIMESTAMPS);
        return new Snapshot(registry, versions, timestamps, VERSION.get());
    }

    /**
     * 将快照信息恢复作为当前节点的注册信息
     * 注意：一样要保证原子性，然后快照恢复期间要禁止数据插入变更，保证数据不丢失
     * @param snapshot
     * @return 返回当前节点快照信息的版本
     */
    public static synchronized long snapshotRestore(Snapshot snapshot) {
        REGISTRY.clear();
        REGISTRY.putAll(snapshot.getREGISTRY());
        VERSIONS.clear();
        VERSIONS.putAll(snapshot.getVERSIONS());
        TIMESTAMPS.clear();
        TIMESTAMPS.putAll(snapshot.getTIMESTAMPS());
        VERSION.set(snapshot.getVersion());
        return snapshot.getVersion();
    }
}
