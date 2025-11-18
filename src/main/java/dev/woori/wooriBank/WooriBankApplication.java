package dev.woori.wooriBank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing  // JPA Auditing 활성화 (BaseEntity의 createdAt, updatedAt 자동 관리)
@SpringBootApplication
public class WooriBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(WooriBankApplication.class, args);
	}

}
