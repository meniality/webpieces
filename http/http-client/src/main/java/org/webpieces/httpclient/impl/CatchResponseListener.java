package org.webpieces.httpclient.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.data.api.DataWrapper;
import org.webpieces.httpclient.api.ResponseListener;
import org.webpieces.httpparser.api.dto.HttpRequest;
import org.webpieces.httpparser.api.dto.HttpResponse;

import java.util.concurrent.CompletableFuture;

public class CatchResponseListener implements ResponseListener {

	private static final Logger log = LoggerFactory.getLogger(CatchResponseListener.class);
	
	private ResponseListener listener;

	public CatchResponseListener(ResponseListener listener) {
		this.listener = listener;
	}

	@Override
	public void incomingResponse(HttpResponse resp, boolean isComplete) {
		try {
			listener.incomingResponse(resp, isComplete);
		} catch(Throwable e) {
			log.error("exception", e);
		}
	}

	@Override
	public void incomingResponse(HttpResponse resp, HttpRequest req, boolean isComplete) {
		try {
			listener.incomingResponse(resp, req, isComplete);
		} catch(Throwable e) {
			log.error("exception", e);
		}
	}

	@Override
	public CompletableFuture<Integer> incomingData(DataWrapper data, boolean isLastData) {
		return listener.incomingData(data, isLastData).exceptionally(e -> {
			log.error("exception", e);
			return data.getReadableSize();
		});
	}

	@Override
	public CompletableFuture<Integer> incomingData(DataWrapper data, HttpRequest request, boolean isLastData) {
		return listener.incomingData(data, request, isLastData).exceptionally(e -> {
			log.error("exception", e);
			return data.getReadableSize();
		});
	}

	@Override
	public void failure(Throwable e) {
		try {
			listener.failure(e);
		} catch(Throwable ee) {
			log.error("exception", ee);
		}			
	}

}
