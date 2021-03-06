package org.webpieces.webserver.test.http2.direct;

import javax.net.ssl.SSLEngine;

import org.webpieces.data.api.TwoPools;
import org.webpieces.http2client.api.Http2Client;
import org.webpieces.http2client.api.Http2Socket;
import org.webpieces.http2client.api.Http2SocketListener;
import org.webpieces.http2client.impl.Http2SocketImpl;
import org.webpieces.nio.api.handlers.ConnectionListener;
import org.webpieces.util.time.TimeImpl;
import org.webpieces.webserver.test.MockChannelManager;
import org.webpieces.webserver.test.MockTcpChannel;

import com.webpieces.hpack.api.HpackParser;
import com.webpieces.hpack.api.HpackParserFactory;
import com.webpieces.http2engine.api.client.Http2ClientEngineFactory;
import com.webpieces.http2engine.api.client.Http2Config;
import com.webpieces.http2engine.api.client.InjectionConfig;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * An Http1.1 Client that sits directly on top of the webserver such that you can step into the webserver
 * from the test case to understand the full stack including your application and the platform
 * 
 * @author dhiller
 *
 */
public class DirectHttp2Client implements Http2Client {

	private Http2ClientEngineFactory factory;
	private MockChannelManager mgr;

	public DirectHttp2Client(MockChannelManager mgr) {
		this.mgr = mgr;
		SimpleMeterRegistry metrics = new SimpleMeterRegistry();
		TwoPools pool = new TwoPools("directhttp2.bufferpool", metrics);
		HpackParser hpackParser = HpackParserFactory.createParser(pool, false);
		Http2Config config = new Http2Config();
		InjectionConfig injConfig = new InjectionConfig(hpackParser, new TimeImpl(), config);
		factory = new Http2ClientEngineFactory(injConfig);
	}

	public Http2Socket createHttpSocket(Http2SocketListener socketListener) {
		ConnectionListener listener = mgr.getHttpConnection();
		MockTcpChannel channel = new MockTcpChannel(false);
		
		return new Http2SocketImpl(new DelayedProxy(listener, channel), factory, socketListener);
	}

	public Http2Socket createHttpsSocket(SSLEngine engine, Http2SocketListener socketListener) {
		ConnectionListener listener = mgr.getHttpConnection();
		MockTcpChannel channel = new MockTcpChannel(true);
		
		return new Http2SocketImpl(new DelayedProxy(listener, channel), factory, socketListener);
	}
}
