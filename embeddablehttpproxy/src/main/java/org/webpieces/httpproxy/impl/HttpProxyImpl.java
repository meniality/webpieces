package org.webpieces.httpproxy.impl;

import java.net.InetSocketAddress;

import javax.inject.Inject;

import org.webpieces.frontend.api.FrontendConfig;
import org.webpieces.frontend.api.HttpFrontendManager;
import org.webpieces.frontend.api.HttpServer;
import org.webpieces.httpproxy.api.HttpProxy;
import org.webpieces.httpproxy.impl.chain.Layer4Processor;
import org.webpieces.util.logging.Logger;
import org.webpieces.util.logging.LoggerFactory;

public class HttpProxyImpl implements HttpProxy {

	private static final Logger log = LoggerFactory.getLogger(HttpProxyImpl.class);
	
	@Inject
	private HttpFrontendManager serverMgr;
	@Inject
	private Layer4Processor serverListener;
	
	private HttpServer httpServer;
	
	@Override
	public void start() {
		log.info("starting server");
		InetSocketAddress addr = new InetSocketAddress(8080);
		FrontendConfig config = new FrontendConfig("httpProxy", addr);
		config.asyncServerConfig.functionToConfigureBeforeBind = s -> s.socket().setReuseAddress(true);
		httpServer = serverMgr.createHttpServer(config, serverListener);
		httpServer.start();
		
//		InetSocketAddress sslAddr = new InetSocketAddress(8443);
//		httpsServer = serverMgr.createTcpServer("httpsProxy", sslAddr, sslServerListener);
		log.info("now listening for incoming connections");
	}

	@Override
	public void stop() {
		httpServer.close();
	}

}
