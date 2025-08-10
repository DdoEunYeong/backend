package com.unithon.ddoeunyeong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	@Bean
	public RestTemplate fastApiRestTemplate() {
		int timeoutMs = 30_000;
		var rf = new HttpComponentsClientHttpRequestFactory();
		rf.setConnectTimeout(timeoutMs);
		return new RestTemplate(rf);
	}
}