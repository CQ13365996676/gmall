package com.atguigu.gmall.item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-31 下午 8:16
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.atguigu.gmall")
public class GmallItemWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(GmallItemWebApplication.class, args);
    }
}
