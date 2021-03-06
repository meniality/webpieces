package org.webpieces.frontend2.api;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import org.webpieces.nio.api.channels.TCPServerChannel;

public interface HttpServer {

	CompletableFuture<Void> start();
	
	CompletableFuture<Void> close();

	void enableOverloadMode(ByteBuffer overloadResponse);

	void disableOverloadMode();

	TCPServerChannel getUnderlyingChannel();

}
