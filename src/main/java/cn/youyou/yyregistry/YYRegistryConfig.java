package cn.youyou.yyregistry;

import cn.youyou.yyregistry.service.RegistryService;
import cn.youyou.yyregistry.service.YYRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YYRegistryConfig {

    @Bean
    public RegistryService registryService() {
        return new YYRegistryService();
    }

}
