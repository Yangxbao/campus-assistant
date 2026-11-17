package com.campus.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 湖南工商大学校园助手后端启动类（学生社区服务项目，演示数据非真实校园资料）。
 *
 * 【新手导读】整个后端的"点火开关"。
 * - @SpringBootApplication = 三个注解的合体：
 *     @SpringBootConfiguration（这是个配置类）
 *     @EnableAutoConfiguration（按 classpath 自动装配：有 web 依赖就起 Tomcat、有 redis 依赖就连 Redis…）
 *     @ComponentScan（扫描本类所在包及子包下所有 @Component/@Service/@Controller，注册成 Bean）
 *   所以业务代码必须放在 com.campus.assistant 包（或其子包）下，放外面扫不到！
 * - @MapperScan：告诉 MyBatis-Plus 去哪个包找数据访问接口（mapper 包里的接口会被
 *   动态代理成实现类，代理里就是我们配好的 SQL 执行器，Service 直接 @Autowired 使用）。
 * - 运行：IDEA 里直接点 main 左边绿三角；或命令行 java -jar campus-assistant-backend.jar。
 */
@SpringBootApplication
@MapperScan("com.campus.assistant.mapper")
public class Application {

    public static void main(String[] args) {
        // 一行启动：内嵌 Tomcat 监听 8080（本机被 nginx 占用，现用 SERVER_PORT=18080），
        // 连 MySQL（campus_assistant 库）和 Redis，初始化全部 Controller/Service。
        SpringApplication.run(Application.class, args);
    }
}
