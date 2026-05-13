package com.seveneleven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SevenElevenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SevenElevenApplication.class, args);
    }
}
