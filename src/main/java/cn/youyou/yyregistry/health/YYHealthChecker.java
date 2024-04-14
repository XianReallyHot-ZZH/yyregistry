package cn.youyou.yyregistry.health;

import cn.youyou.yyregistry.model.InstanceMeta;
import cn.youyou.yyregistry.service.RegistryService;
import cn.youyou.yyregistry.service.YYRegistryService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class YYHealthChecker implements HealthChecker {

    RegistryService registryService;

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    // 掉线时长
    long timeout = 20_000;

    public YYHealthChecker(RegistryService registryService) {
        this.registryService = registryService;
    }


    @Override
    public void start() {
        executor.scheduleWithFixedDelay(
                () -> {
                    log.info(" ===> Health checker running...");
                    long now = System.currentTimeMillis();
                    YYRegistryService.TIMESTAMPS.keySet().forEach(serviceAndInst -> {
                        Long timestamp = YYRegistryService.TIMESTAMPS.get(serviceAndInst);
                        if (now - timestamp > timeout) {
                            log.info(" ===> Health checker: {} is down", serviceAndInst);
                            int index = serviceAndInst.indexOf("@");
                            String service = serviceAndInst.substring(0, index);
                            String url = serviceAndInst.substring(index + 1);
                            InstanceMeta instance = InstanceMeta.from(url);
                            registryService.unregister(service, instance);
                            YYRegistryService.TIMESTAMPS.remove(serviceAndInst);
                        }
                    });
                },
                10, 10, TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
