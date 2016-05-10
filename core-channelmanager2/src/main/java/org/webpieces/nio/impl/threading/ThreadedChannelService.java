package org.webpieces.nio.impl.threading;

import java.util.concurrent.Executor;

import org.webpieces.nio.api.ChannelManager;
import org.webpieces.nio.api.channels.DatagramChannel;
import org.webpieces.nio.api.channels.TCPChannel;
import org.webpieces.nio.api.channels.TCPServerChannel;
import org.webpieces.nio.api.channels.UDPChannel;
import org.webpieces.nio.api.handlers.ConnectionListener;
import org.webpieces.nio.api.handlers.DataListener;
import org.webpieces.nio.api.handlers.DatagramListener;
import org.webpieces.util.threading.SessionExecutor;
import org.webpieces.util.threading.SessionExecutorImpl;

public class ThreadedChannelService implements ChannelManager {

	private ChannelManager mgr;
	private SessionExecutor executor;

	public ThreadedChannelService(ChannelManager mgr, Executor executor) {
		this.mgr = mgr;
		this.executor = new SessionExecutorImpl(executor);
	}

	@Override
	public TCPServerChannel createTCPServerChannel(String id, ConnectionListener connectionListener,
			DataListener dataListener) {
		DataListener wrapperDataListener = new ThreadDataListener(dataListener, executor);
		ConnectionListener wrapperConnectionListener = new ThreadConnectionListener(connectionListener, executor);
		//because no methods return futures in this type of class, we do not need to proxy him....
		return mgr.createTCPServerChannel(id, wrapperConnectionListener , wrapperDataListener );
	}

	@Override
	public TCPChannel createTCPChannel(String id, DataListener listener) {
		TCPChannel channel = mgr.createTCPChannel(id, new ThreadDataListener(listener, executor));
		return new ThreadTCPChannel(channel, executor);
	}

	@Override
	public UDPChannel createUDPChannel(String id, DataListener listener) {
		UDPChannel channel = mgr.createUDPChannel(id, new ThreadDataListener(listener, executor));
		return new ThreadUDPChannel(channel, executor);
	}

	@Override
	public DatagramChannel createDatagramChannel(String id, int bufferSize, DatagramListener listener) {
		return mgr.createDatagramChannel(id, bufferSize, new ThreadDatagramListener(listener, executor));
	}

	@Override
	public void stop() {
		mgr.stop();
	}

}