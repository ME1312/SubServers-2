package net.ME1312.SubServers.Web.Logging;

import net.ME1312.SubServers.Web.SubAPI;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class SLF4JLoggerProvider implements SLF4JServiceProvider {
	@Override
	public ILoggerFactory getLoggerFactory() {
		return new SLF4JLoggerFactory();
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return null;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return null;
	}

	@Override
	public String getRequestedApiVersion() {
		return "2.0"; //SLF4J api version
	}

	@Override
	public void initialize() {

	}
}
