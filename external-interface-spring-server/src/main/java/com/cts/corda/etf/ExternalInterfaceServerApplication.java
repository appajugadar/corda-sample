package com.cts.corda.etf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
public class ExternalInterfaceServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExternalInterfaceServerApplication.class, args);
    }

}