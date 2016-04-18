package org.webpieces.asyncserver.api;

import org.webpieces.asyncserver.impl.AsyncServerManagerImpl;
import org.webpieces.nio.api.BufferCreationPool;
import org.webpieces.nio.api.ChannelManager;
import org.webpieces.nio.api.ChannelManagerFactory;

public class AsyncServerMgrFactory {

	public static AsyncServerManager createAsyncServer(String id, BufferCreationPool pool) {
		ChannelManagerFactory factory = ChannelManagerFactory.createFactory();
		ChannelManager mgr = factory.createChannelManager(id, pool);
		return createAsyncServer(mgr);
	}
	
	public static AsyncServerManager createAsyncServer(ChannelManager channelManager) {
		return new AsyncServerManagerImpl(channelManager);
	}
	
}
