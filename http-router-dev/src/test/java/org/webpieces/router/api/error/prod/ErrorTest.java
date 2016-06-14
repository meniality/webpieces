package org.webpieces.router.api.error.prod;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.router.api.RouterSvcFactory;
import org.webpieces.router.api.RoutingService;
import org.webpieces.router.api.dto.HttpMethod;
import org.webpieces.router.api.dto.Request;
import org.webpieces.router.api.error.dev.NoMethodRouterModules;
import org.webpieces.router.api.error.dev.TooManyArgsRouterModules;
import org.webpieces.router.api.mocks.MockResponseStream;
import org.webpieces.router.api.mocks.VirtualFileInputStream;
import org.webpieces.util.file.VirtualFile;

public class ErrorTest {
	
	private static final Logger log = LoggerFactory.getLogger(ErrorTest.class);

	@Test
	public void testNoMethod() {
		log.info("starting");
		String moduleFileContents = NoMethodRouterModules.class.getName();
		RoutingService server = createServer(moduleFileContents);

		try {
			server.start();
			Assert.fail("Should have thrown exception on start since this is prod");
		} catch(RuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("Cannot find 'public' method=thisMethodNotExist on class="));
		}
		
		try {
			Request req = createHttpRequest(HttpMethod.GET, "/something");
			server.processHttpRequests(req, new MockResponseStream());
			Assert.fail("should have thrown");
		} catch(IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("start was not called by client or start threw"));
		}
		
	}
	
	@Test
	public void testArgsMismatch() {
		log.info("starting");
		String moduleFileContents = TooManyArgsRouterModules.class.getName();
		RoutingService server = createServer(moduleFileContents);
		
		server.start();
		
		try {
			Request req = createHttpRequest(HttpMethod.GET, "/something");
			server.processHttpRequests(req, new MockResponseStream());
			Assert.fail("should have thrown");
		} catch(IllegalStateException e) {
			Assert.assertTrue(e.getMessage().contains("The method='argsMismatch' takes 2 arguments"));
		}
	}

	private RoutingService createServer(String moduleFileContents) {
		VirtualFile f = new VirtualFileInputStream(moduleFileContents.getBytes(), "testAppModules");		
		RoutingService server = RouterSvcFactory.create(f);
		return server;
	}

	private Request createHttpRequest(HttpMethod method, String path) {
		Request r = new Request();
		r.method = method;
		r.relativePath = path;
		
		return r;
	}

}