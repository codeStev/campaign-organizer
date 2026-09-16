package com.campaignorganizer.config.db;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wraps Spring Boot's autoconfigured {@code dataSource} bean in
 * {@link GucSettingDataSource} (ADR-0114) — Hikari pool construction/sizing
 * stays entirely Boot's own, this only intercepts the connections it hands
 * out. Flyway's separately-configured connection ({@code spring.flyway.*},
 * a different bean entirely) is never touched here — correct, since it runs
 * as the migrations-only superuser with no per-request account to tag.
 *
 * <p>{@code static}, per Spring's own guidance for {@link BeanPostProcessor}
 * beans: this one needs no dependencies of its own, so declaring it
 * non-static would force the whole configuration class to instantiate
 * earlier than necessary.
 */
@Configuration
public class DataSourceGucConfig {

    @Bean
    static BeanPostProcessor gucSettingDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if ("dataSource".equals(beanName) && bean instanceof DataSource dataSource
                        && !(bean instanceof GucSettingDataSource)) {
                    return new GucSettingDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
