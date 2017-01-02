package com.webpieces.http2engine.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;
import org.webpieces.data.api.DataWrapperGeneratorFactory;

import com.twitter.hpack.Encoder;
import com.webpieces.http2parser.api.dto.ContinuationFrame;
import com.webpieces.http2parser.api.dto.PushPromiseFrame;
import com.webpieces.http2parser.api.dto.lib.HasHeaderFragment;
import com.webpieces.http2parser.api.dto.lib.Http2Frame;
import com.webpieces.http2parser.api.dto.lib.Http2Header;

public class HeaderEncoding {
    private static final DataWrapperGenerator dataGen = DataWrapperGeneratorFactory.createDataWrapperGenerator();
	private Encoder encoder;
	private int maxFrameSize;

    public HeaderEncoding(Encoder encoder, int maxFrameSize) {
		this.encoder = encoder;
		this.maxFrameSize = maxFrameSize;
    }
    
    public List<Http2Frame> createPushPromises(List<Http2Header> headers, int streamId, int promisedStreamId) {
    	PushPromiseFrame promise = new PushPromiseFrame();
    	promise.setStreamId(streamId);
    	promise.setPromisedStreamId(promisedStreamId);
    	
    	return createHeaderFrames(promise, headers);
    }
    
    public List<Http2Frame> createHeaderFrames(HasHeaderFragment initialFrame, List<Http2Header> headers) {
        List<Http2Frame> headerFrames = new LinkedList<>();
    	
        DataWrapper serializedHeaders = serializeHeaders(headers);

        HasHeaderFragment currentFrame = initialFrame;
        HasHeaderFragment lastFrame = currentFrame;
        DataWrapper dataLeftOver = serializedHeaders;
        while(dataLeftOver.getReadableSize() > 0) {
            lastFrame = currentFrame;
        	int splitSize = Math.min(dataLeftOver.getReadableSize(), maxFrameSize);
            List<? extends DataWrapper> split = dataGen.split(dataLeftOver, splitSize);
            DataWrapper fragment = split.get(0);
            
            currentFrame.setHeaderFragment(fragment);
            headerFrames.add(currentFrame);
            
            currentFrame = new ContinuationFrame();
            currentFrame.setStreamId(initialFrame.getStreamId());
            dataLeftOver = split.get(1);
        }

        //last frame is currentFrame so set end header
        lastFrame.setEndHeaders(true);
		return headerFrames;
	}
    
    public DataWrapper serializeHeaders(List<Http2Header> headers) {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Http2Header header : headers) {
            try {
                encoder.encodeHeader(
                        out,
                        header.getName().toLowerCase().getBytes(),
                        header.getValue().getBytes(),
                        false);
            } catch (IOException e) {
            	throw new RuntimeException(e);
            }
        }
        return dataGen.wrapByteArray(out.toByteArray());
    }

	public void setMaxHeaderTableSize(int value) {
		try {
			ByteArrayOutputStream str = new ByteArrayOutputStream();
			encoder.setMaxHeaderTableSize(str, value);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setMaxFrameSize(int value) {
		this.maxFrameSize = value;
	}
}
