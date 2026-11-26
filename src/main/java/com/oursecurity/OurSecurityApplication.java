package com.oursecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OurSecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(OurSecurityApplication.class, args);
    }
}
