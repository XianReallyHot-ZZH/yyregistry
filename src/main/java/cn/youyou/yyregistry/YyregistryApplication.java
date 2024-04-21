package cn.youyou.yyregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = {YYRegistryConfigProperties.class})
public class YyregistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(YyregistryApplication.class, args);
    }

}
