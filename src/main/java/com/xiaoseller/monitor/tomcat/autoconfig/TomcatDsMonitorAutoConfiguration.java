package com.xiaoseller.monitor.tomcat.autoconfig;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xiaoseller.monitor.tomcat.jdbc.DataSourceMonitor;

@Configuration
@ConditionalOnClass({ DataSource.class })
public class TomcatDsMonitorAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	public DataSourceMonitor dataSourceMonitor() {
		return new DataSourceMonitor();
	}
}
