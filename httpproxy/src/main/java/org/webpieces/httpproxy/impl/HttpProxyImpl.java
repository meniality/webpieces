package org.webpieces.httpproxy.impl;

import javax.inject.Inject;

import org.webpieces.httpproxy.api.HttpProxy;
import org.webpieces.httpproxy.impl.chain.Layer1ConnectionListener;
import org.webpieces.httpproxy.impl.chain.SSLLayer1ConnectionListener;
import org.webpieces.nio.api.ChannelManager;
import org.webpieces.nio.api.channels.TCPServerChannel;

public class HttpProxyImpl implements HttpProxy {

	@Inject
	private ChannelManager channelManager;
	@Inject
	private Layer1ConnectionListener serverListener;
	@Inject
	private SSLLayer1ConnectionListener sslServerListener;
	
	private TCPServerChannel serverChannel;
	private TCPServerChannel sslServerChannel;
	
	@Override
	public void start() {
		serverChannel = channelManager.createTCPServerChannel("httpProxy");
		serverChannel.registerServerSocketChannel(serverListener);

		sslServerChannel = channelManager.createTCPServerChannel("httpsProxy");
		sslServerChannel.registerServerSocketChannel(sslServerListener);
	}

	@Override
	public void stop() {
		serverChannel.closeServerChannel();
		sslServerChannel.closeServerChannel();
		
		throw new UnsupportedOperationException("close all tcp channels too");
	}

}