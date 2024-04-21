package cn.youyou.yyregistry.cluster;

import cn.youyou.yyregistry.model.InstanceMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

/**
 * 节点的注册信息快照
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Snapshot {

    // 服务的注册实例信息
    LinkedMultiValueMap<String, InstanceMeta> REGISTRY;

    // 服务的版本号，用版本号来表达服务发生了变化，比如实例数
    Map<String, Long> VERSIONS;

    // 服务@实例的心跳
    Map<String, Long> TIMESTAMPS;

    // 版本发号器的最新版本号
    Long version;

}
