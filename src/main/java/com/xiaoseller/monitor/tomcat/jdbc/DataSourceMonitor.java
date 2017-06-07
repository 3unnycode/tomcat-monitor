package com.xiaoseller.monitor.tomcat.jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.TomcatDataSourcePoolMetadata;
import org.springframework.context.ApplicationContext;

import com.xiaoseller.monitor.Monitor;
import com.xiaoseller.monitor.MonitorFactory;
import com.xiaoseller.monitor.MonitorPoint;

public class DataSourceMonitor {
	@Value("${app.code}")
	private String systemCode;
	private static final String DATASOURCE_SUFFIX = "dataSource";
	@Autowired
	private ApplicationContext applicationContext;
	private final Map<String, DataSourcePoolMetadata> metadataByPrefix = new HashMap();

	@PostConstruct
	public void initialize() {
		DataSource primaryDataSource = this.getPrimaryDataSource();
		Iterator arg1 = this.applicationContext.getBeansOfType(DataSource.class).entrySet().iterator();

		while (arg1.hasNext()) {
			Entry entry = (Entry) arg1.next();
			String beanName = (String) entry.getKey();
			DataSource bean = (DataSource) entry.getValue();
			String prefix = this.createPrefix(beanName, bean, bean.equals(primaryDataSource));
			TomcatDataSourcePoolMetadata poolMetadata = new TomcatDataSourcePoolMetadata(bean);
			if (poolMetadata != null) {
				this.metadataByPrefix.put(prefix, poolMetadata);
			}
		}

		(new ScheduledThreadPoolExecutor(1)).scheduleAtFixedRate(new Runnable() {
			public void run() {
				DataSourceMonitor.this.monitor();
			}
		}, 1000L, 1000L, TimeUnit.MILLISECONDS);
	}

	public void monitor() {
		Iterator arg0 = this.metadataByPrefix.entrySet().iterator();

		while (arg0.hasNext()) {
			Entry entry = (Entry) arg0.next();
			String prefix = (String) entry.getKey();
			String datasourceId = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
			DataSourcePoolMetadata metadata = (DataSourcePoolMetadata) entry.getValue();
			Monitor monitor = MonitorFactory.connect();
			monitor.writePoint(
					MonitorPoint.monitorKey("wireless.datasouce.status").addTag("systemCode", this.systemCode)
							.addTag("datasource", datasourceId).addField("active", metadata.getActive())
							.addField("usage", metadata.getUsage().toString()).build());
		}

	}

	protected String createPrefix(String name, DataSource dataSource, boolean primary) {
		if (primary) {
			return "datasource.primary";
		} else {
			if (name.length() > "dataSource".length() && name.toLowerCase().endsWith("dataSource".toLowerCase())) {
				name = name.substring(0, name.length() - "dataSource".length());
			}

			return "datasource." + name;
		}
	}

	private DataSource getPrimaryDataSource() {
		try {
			return (DataSource) this.applicationContext.getBean(DataSource.class);
		} catch (NoSuchBeanDefinitionException arg1) {
			return null;
		}
	}
}
