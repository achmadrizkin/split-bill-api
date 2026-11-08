package com.achmadrizkin.splitbill.infrastructure.config;

import com.achmadrizkin.splitbill.domain.charge.ServiceChargePolicy;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /** Injected instead of calling Instant.now() so time is controllable in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ServiceChargePolicy serviceChargePolicy(AppProperties properties) {
        ServiceChargePolicy policy = ServiceChargePolicy.forGithubUsername(properties.githubUsername());
        log.info("Service charge for GitHub user '{}' is {}%", properties.githubUsername(), policy.percentage());
        return policy;
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Split Bill API")
                .version("v1")
                .description("Manage shared expenses and settle who owes whom. "
                        + "Monetary amounts are exchanged as decimal strings, e.g. \"150000.00\"."));
    }
}
