package net.ME1312.SubServers.Web.Logging;

import org.slf4j.Logger;

public class SLF4JLoggerFactory implements org.slf4j.ILoggerFactory {
	@Override
	public Logger getLogger(String s) {
		return new SLF4JLogger(new net.ME1312.Galaxi.Log.Logger(s));
	}
}
