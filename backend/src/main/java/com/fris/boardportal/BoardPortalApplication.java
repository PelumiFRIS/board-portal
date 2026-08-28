package com.fris.boardportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoardPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardPortalApplication.class, args);
	}

}
