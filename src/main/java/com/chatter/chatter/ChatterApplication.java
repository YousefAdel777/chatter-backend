package com.chatter.chatter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ChatterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatterApplication.class, args);
	}

}
