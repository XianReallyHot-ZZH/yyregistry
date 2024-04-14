package cn.youyou.yyregistry.service;

import cn.youyou.yyregistry.model.InstanceMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


@Slf4j
public class YYRegistryService implements RegistryService {

    // 服务的注册实例信息
    final static MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>();

    // 服务的版本号
    final static Map<String, Long> VERSIONS = new ConcurrentHashMap<>();

    // 服务@实例的心跳
    public final static Map<String, Long> TIMESTAMPS = new ConcurrentHashMap<>();

    // 服务版本号发号器
    final static AtomicLong VERSION = new AtomicLong(0);


    @Override
    public InstanceMeta register(String service, InstanceMeta instance) {
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
    public InstanceMeta unregister(String service, InstanceMeta instance) {
        List<InstanceMeta> instanceMetas = REGISTRY.get(service);
        if (instanceMetas == null || instanceMetas.isEmpty()) {
            return null;
        }
        log.info(" ====> unregister instance {}", instance.toUrl());
        instanceMetas.removeIf(instanceMeta -> instanceMeta.equals(instance));
        instance.setStatus(false);
//        renew(instance, service);
//        VERSIONS.put(service, VERSION.incrementAndGet());
        return instance;
    }

    @Override
    public List<InstanceMeta> getAllInstances(String service) {
        return REGISTRY.get(service);
    }

    @Override
    public Long renew(InstanceMeta instance, String... services) {
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
}
