package com.tanle.t_shorten_url;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TShortenUrlApplication {

	public static void main(String[] args) {
		SpringApplication.run(TShortenUrlApplication.class, args);
	}

}
