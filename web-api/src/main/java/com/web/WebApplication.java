package com.web;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@RequiredArgsConstructor
public class WebApplication{

    static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
