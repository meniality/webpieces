package org.webpieces.http2client.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.webpieces.http2parser.api.dto.lib.Http2Header;

public class Http2Headers implements PartialResponse {

	private int streamId;
	private boolean lastPartOfResponse = false;
	protected List<Http2Header> headers = new ArrayList<>();
	//Convenience structure that further morphs the headers into a Map that can
	//be looked up by key.
	private transient Http2HeaderStruct headersStruct = new Http2HeaderStruct();

	public Http2Headers() {}
	public Http2Headers(List<Http2Header> headerList) {
		for(Http2Header header : headerList) {
			addHeader(header);
		}
	}

	public int getStreamId() {
		return streamId;
	}

	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}
	
	/**
	 * Order of HTTP Headers matters for Headers with the same key
	 */
	public List<Http2Header> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	public void addHeader(Http2Header header) {
		headers.add(header);
		headersStruct.addHeader(header);
	}
	
	/** 
	 * 
	 * @return
	 */
	public Http2HeaderStruct getHeaderLookupStruct() {
		return headersStruct;
	}
	public boolean isLastPartOfResponse() {
		return lastPartOfResponse;
	}
	public void setLastPartOfResponse(boolean lastPartOfResponse) {
		this.lastPartOfResponse = lastPartOfResponse;
	}
	
	@Override
	public String toString() {
		return "Http2Headers [streamId=" + streamId + ", lastPartOfResponse=" + lastPartOfResponse + ", headers="
				+ headers + "]";
	}
}