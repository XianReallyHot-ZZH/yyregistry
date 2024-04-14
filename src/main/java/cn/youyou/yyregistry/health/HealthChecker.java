package cn.youyou.yyregistry.health;

/**
 * 健康检查
 * 负责对注册上来的实例进行健康检查，实现方式是检查实例心跳
 */
public interface HealthChecker {

    void start();

    void stop();

}
