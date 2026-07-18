package com.paymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync        // needed for async webhook delivery
@EnableScheduling   // needed for webhook retry scheduler
public class PaymentGatewayApplication {
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(PaymentGatewayApplication.class, args);
	}
}
