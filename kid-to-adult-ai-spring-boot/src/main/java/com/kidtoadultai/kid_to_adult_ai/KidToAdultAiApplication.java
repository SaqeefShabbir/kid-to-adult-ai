package com.kidtoadultai.kid_to_adult_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KidToAdultAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(KidToAdultAiApplication.class, args);
	}

}
