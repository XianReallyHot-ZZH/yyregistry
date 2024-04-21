package cn.youyou.yyregistry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "yyregistry")
public class YYRegistryConfigProperties {

    private List<String> serverList;

}
