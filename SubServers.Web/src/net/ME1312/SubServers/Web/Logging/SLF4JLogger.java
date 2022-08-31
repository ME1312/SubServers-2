package net.ME1312.SubServers.Web.Logging;

import net.ME1312.Galaxi.Log.Logger;
import org.slf4j.Marker;

public class SLF4JLogger implements org.slf4j.Logger{
	private Logger logger;

	public SLF4JLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public String getName() {
		return logger.getPrefix();
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void trace(String s) {}

	@Override
	public void trace(String s, Object o) {}

	@Override
	public void trace(String s, Object o, Object o1) {}

	@Override
	public void trace(String s, Object... objects) {}

	@Override
	public void trace(String s, Throwable throwable) {}

	@Override
	public boolean isTraceEnabled(Marker marker) {return false;}

	@Override
	public void trace(Marker marker, String s) {}

	@Override
	public void trace(Marker marker, String s, Object o) {}

	@Override
	public void trace(Marker marker, String s, Object o, Object o1) {}

	@Override
	public void trace(Marker marker, String s, Object... objects) {}

	@Override
	public void trace(Marker marker, String s, Throwable throwable) {}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public void debug(String s) {
		logger.debug.println(s);
	}

	@Override
	public void debug(String s, Object o) {
		logger.debug.print(s);
		logger.debug.println(o);
	}

	@Override
	public void debug(String s, Object o, Object o1) {
		logger.debug.print(s);
		logger.debug.print(o);
		logger.debug.println(o1);
	}

	@Override
	public void debug(String s, Object... objects) {
		logger.debug.print(s);
		logger.debug.println(objects);
	}

	@Override
	public void debug(String s, Throwable throwable) {
		logger.debug.print(s);
		logger.debug.println(throwable);
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {return false;}

	@Override
	public void debug(Marker marker, String s) {}

	@Override
	public void debug(Marker marker, String s, Object o) {}

	@Override
	public void debug(Marker marker, String s, Object o, Object o1) {}

	@Override
	public void debug(Marker marker, String s, Object... objects) {}

	@Override
	public void debug(Marker marker, String s, Throwable throwable) {}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public void info(String s) {
		logger.info.println(s);
	}

	@Override
	public void info(String s, Object o) {
		logger.info.print(s);
		logger.info.println(o);
	}

	@Override
	public void info(String s, Object o, Object o1) {
		logger.info.print(s);
		logger.info.print(s);
		logger.info.println(o1);
	}

	@Override
	public void info(String s, Object... objects) {
		logger.info.print(s);
		logger.info.println(objects);
	}

	@Override
	public void info(String s, Throwable throwable) {
		logger.info.print(s);
		logger.info.println(throwable);
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {return false;}

	@Override
	public void info(Marker marker, String s) {}

	@Override
	public void info(Marker marker, String s, Object o) {}

	@Override
	public void info(Marker marker, String s, Object o, Object o1) {}

	@Override
	public void info(Marker marker, String s, Object... objects) {}

	@Override
	public void info(Marker marker, String s, Throwable throwable) {}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public void warn(String s) {
		logger.warn.println(s);
	}

	@Override
	public void warn(String s, Object o) {
		logger.warn.print(s);
		logger.warn.println(o);
	}

	@Override
	public void warn(String s, Object... objects) {
		logger.warn.print(s);
		logger.warn.println(objects);
	}

	@Override
	public void warn(String s, Object o, Object o1) {
		logger.warn.print(s);
		logger.warn.println(o);
		logger.warn.println(o1);
	}

	@Override
	public void warn(String s, Throwable throwable) {
		logger.warn.print(s);
		logger.warn.println(throwable);
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {return false;}

	@Override
	public void warn(Marker marker, String s) {}

	@Override
	public void warn(Marker marker, String s, Object o) {}

	@Override
	public void warn(Marker marker, String s, Object o, Object o1) {}

	@Override
	public void warn(Marker marker, String s, Object... objects) {}

	@Override
	public void warn(Marker marker, String s, Throwable throwable) {}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public void error(String s) {
		logger.error.println(s);
	}

	@Override
	public void error(String s, Object o) {
		logger.error.print(s);
		logger.error.println(o);
	}

	@Override
	public void error(String s, Object o, Object o1) {
		logger.error.print(s);
		logger.error.print(o);
		logger.error.println(o1);
	}

	@Override
	public void error(String s, Object... objects) {
		logger.error.print(s);
		logger.error.println(objects);
	}

	@Override
	public void error(String s, Throwable throwable) {
		logger.error.println(s);
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {return false;}

	@Override
	public void error(Marker marker, String s) {}

	@Override
	public void error(Marker marker, String s, Object o) {}

	@Override
	public void error(Marker marker, String s, Object o, Object o1) {}

	@Override
	public void error(Marker marker, String s, Object... objects) {}

	@Override
	public void error(Marker marker, String s, Throwable throwable) {}
}
