package com.orangelan.resolinkagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResolinkAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResolinkAgentApplication.class, args);
		
		// 打印代理设置
		System.out.println("HTTP proxy host: " + System.getProperty("http.proxyHost"));
		System.out.println("HTTP proxy port: " + System.getProperty("http.proxyPort"));
		System.out.println("HTTPS proxy host: " + System.getProperty("https.proxyHost"));
		System.out.println("HTTPS proxy port: " + System.getProperty("https.proxyPort"));
	}
}