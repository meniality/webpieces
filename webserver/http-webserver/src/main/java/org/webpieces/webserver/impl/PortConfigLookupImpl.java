package org.webpieces.webserver.impl;

import org.webpieces.router.api.PortConfig;
import org.webpieces.router.api.PortConfigLookup;

/**
 * The router is it's own piece and does not depend on the webserver, BUT the router is started before
 * the webserver since we have to be ready to serve all pages.  If the server binds to port 0, the
 * router will need to get this information AFTER starting up.  So, the order is
 * 
 * router starts but needs port info(port not available until webserver binds address)
 * webserver ports are opened to world to serve up pages(now that router is ready to serve pages)
 * WebserverImpl binds to port 0 or 8080/8443, etc.  If 0, port is known AFTER the call to bind
 * WebserverImpl gets port info and sets the port info into PortConfigLookupImpl immediately
 * Router reads the port information later as requests come in
 * 
 * PortConfigLookup is something the Router needs and WebServerPortInformation is something the
 * webserver needs to fill in
 * 
 * @return
 */
public class PortConfigLookupImpl implements WebServerPortInformation, PortConfigLookup {

	private PortConfig portConfig;

	@Override
	public void setPortConfig(PortConfig portConfig) {
		this.portConfig = portConfig;
	}

	@Override
	public PortConfig getPortConfig() {
		return portConfig;
	}
	
}
