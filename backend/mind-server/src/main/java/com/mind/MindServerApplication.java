package com.mind;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 思维赋能教学辅助系统 - Java 后端启动类
 * 聚合 mind-system / mind-knowledge / mind-agent / mind-analysis 四个业务模块
 */
@SpringBootApplication(scanBasePackages = "com.mind")
@MapperScan("com.mind.**.mapper")
public class MindServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindServerApplication.class, args);
    }
}
