package dev.woori.wooriBank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WooriBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(WooriBankApplication.class, args);
	}

}
