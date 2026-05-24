package com.solidoa.collaboration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.solidoa.collaboration.mapper")
public class CollabApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollabApplication.class, args);
    }
}