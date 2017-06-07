package com.xiaoseller.monitor.tomcat.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.jdbc.pool.PoolProperties.InterceptorProperty;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaoseller.monitor.Monitor;
import com.xiaoseller.monitor.MonitorFactory;
import com.xiaoseller.monitor.MonitorPoint;

public class SlowQuerySqlMonitor extends SlowQueryReport {
	private String systemCode;
	private static final Logger LOGGER = LoggerFactory.getLogger(SlowQuerySqlMonitor.class);

	public void setProperties(Map<String, InterceptorProperty> properties) {
		super.setProperties(properties);
		String systemCode = "systemCode";
		InterceptorProperty p1 = (InterceptorProperty) properties.get("systemCode");
		if (p1 != null) {
			this.setSystemCode(p1.getValue());
		}

	}

	protected String reportSlowQuery(String query, Object[] args, String name, long start, long delta) {
		String sql = query == null && args != null && args.length > 0 ? (String) args[0] : query;
		if (sql == null && this.compare("executeBatch", name)) {
			sql = "batch";
		}

		if (this.isLogSlow() && sql != null) {
			String beautifulSql = sql.replace("\n", "").replaceAll("[\' \']+", " ");
			LOGGER.warn("Slow Query Report SQL={}; param:[{}], consume={};", new Object[] { beautifulSql,
					StringUtils.join((Iterable) ParamHolder.params.get(), ','), Long.valueOf(delta) });
			Monitor monitor = MonitorFactory.connect();
			monitor.writePoint(MonitorPoint.monitorKey("wireless.sql.slowlog").addTag("systemCode", this.systemCode)
					.addField("cost", Long.valueOf(delta)).addTag("sql", DigestUtils.md5Hex(beautifulSql)).build());
		}

		return sql;
	}

	public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
		try {
			Object x = null;
			String name = method.getName();
			String sql = null;
			Constructor constructor = null;
			if (this.compare("createStatement", name)) {
				constructor = this.getConstructor(0, Statement.class);
			} else if (this.compare("prepareStatement", name)) {
				sql = (String) args[0];
				constructor = this.getConstructor(1, PreparedStatement.class);
				if (sql != null) {
					this.prepareStatement(sql, time);
				}
			} else {
				if (!this.compare("prepareCall", name)) {
					return statement;
				}

				sql = (String) args[0];
				constructor = this.getConstructor(2, CallableStatement.class);
				this.prepareCall(sql, time);
			}

//			x = constructor
//					.newInstance(new Object[] { new SlowQuerySqlMonitor.RecordParamStatementProxy(statement, sql) });
			return null;
		} catch (Exception arg10) {
			LOGGER.warn("Unable to create statement proxy for slow query report.", arg10);
			return statement;
		}
	}

	public String getSystemCode() {
		return this.systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public class RecordParamStatementProxy extends StatementProxy {

		public RecordParamStatementProxy(Object paramObject, String paramString) {
			super(paramObject, paramString);
		}
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().startsWith("set") && args != null && args.length >= 2) {
				((List) ParamHolder.params.get()).add(args[1]);
			}

			Object result = null;

			try {
				result = super.invoke(proxy, method, args);
			} finally {
				if (SlowQuerySqlMonitor.this.isExecute(method, false)) {
					ParamHolder.params.remove();
				}

			}

			return result;
		}
	}
}
