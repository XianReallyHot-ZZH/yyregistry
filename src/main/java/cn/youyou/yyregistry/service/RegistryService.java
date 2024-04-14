package cn.youyou.yyregistry.service;

import cn.youyou.yyregistry.model.InstanceMeta;

import java.util.List;
import java.util.Map;

/**
 * 注册中心的核心服务能力接口
 */
public interface RegistryService {

    // ================== 基础功能 ===================
    InstanceMeta register(String service, InstanceMeta instance);

    InstanceMeta unregister(String service, InstanceMeta instance);

    List<InstanceMeta> getAllInstances(String service);

    // ================== 高级功能 ===================
    // 刷新实例心跳，返回的是时间戳
    Long renew(InstanceMeta instance, String... services);

    // 获取服务版本号
    Long version(String service);

    // 批量获取服务版本号
    Map<String, Long> versions(String... services);



}
