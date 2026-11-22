package com.rankedfinance;

import org.springframework.boot.SpringApplication;

public class TestRankedFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.from(RankedFinanceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
