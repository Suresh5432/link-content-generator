package com.linklens.summarizationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SummarizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SummarizationServiceApplication.class, args);
    }

}
