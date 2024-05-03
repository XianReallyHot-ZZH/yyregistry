package cn.youyou.yyregistry;

import cn.youyou.yyregistry.cluster.Cluster;
import cn.youyou.yyregistry.health.HealthChecker;
import cn.youyou.yyregistry.health.YYHealthChecker;
import cn.youyou.yyregistry.service.RegistryService;
import cn.youyou.yyregistry.service.YYRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YYRegistryConfig {

    @Bean
    public RegistryService registryService() {
        return new YYRegistryService();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public HealthChecker healthChecker(@Autowired RegistryService registryService) {
        return new YYHealthChecker(registryService);
    }

    @Bean(initMethod = "init")
    public Cluster cluster(@Autowired YYRegistryConfigProperties registryConfigProperties) {
        return new Cluster(registryConfigProperties);
    }

}
