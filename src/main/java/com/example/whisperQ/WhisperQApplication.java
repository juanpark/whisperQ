package com.example.whisperQ;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@EnableScheduling
@SpringBootApplication
public class WhisperQApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhisperQApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// Set JVM default timezone to Korea
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

}
