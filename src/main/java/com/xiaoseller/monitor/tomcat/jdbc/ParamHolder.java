package com.xiaoseller.monitor.tomcat.jdbc;

import java.util.ArrayList;
import java.util.List;

public class ParamHolder {
	
	public static ThreadLocal<List<Object>> params = new ThreadLocal() {
		protected List<Object> initialValue() {
			return new ArrayList();
		}
	};
}
