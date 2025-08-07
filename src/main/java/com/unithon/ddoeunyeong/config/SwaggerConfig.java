package com.unithon.ddoeunyeong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {
	@Bean
	public OpenAPI openAPI() {
		SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");

		return new OpenAPI()
			.components(new Components())
			.info(apiInfo());
			// .addSecurityItem(securityRequirement)
			// .schemaRequirement("BearerAuth", securityScheme());
	}

	private Info apiInfo() {
		return new Info()
			.title("또! 오은영팀 API")
			.description("또! 오은영팀 API 명세서입니다")
			.version("1.0.0");
	}

	private SecurityScheme securityScheme() {
		return new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER)
			.name("Authorization");
	}
}
