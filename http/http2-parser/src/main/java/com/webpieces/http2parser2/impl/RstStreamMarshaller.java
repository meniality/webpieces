package com.webpieces.http2parser2.impl;

import java.nio.ByteBuffer;

import org.webpieces.data.api.BufferPool;
import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;

import com.webpieces.http2parser.api.dto.Http2ErrorCode;
import com.webpieces.http2parser.api.dto.Http2Frame;
import com.webpieces.http2parser.api.dto.Http2RstStream;

public class RstStreamMarshaller extends AbstractFrameMarshaller implements FrameMarshaller {
	RstStreamMarshaller(BufferPool bufferPool, DataWrapperGenerator dataGen) {
		super(bufferPool, dataGen);
	}

	@Override
	public DataWrapper marshal(Http2Frame frame) {
		Http2RstStream castFrame = (Http2RstStream) frame;

		ByteBuffer payload = bufferPool.nextBuffer(4);
		payload.putInt(castFrame.getErrorCode().getCode());
		payload.flip();

		DataWrapper dataPayload = dataGen.wrapByteBuffer(payload);
		return super.createFrame(frame, (byte) 0, dataPayload);
	}

	@Override
	public Http2Frame unmarshal(Http2MementoImpl state, DataWrapper framePayloadData) {
		Http2RstStream frame = new Http2RstStream();
		super.fillInFrameHeader(state, frame);

		ByteBuffer payloadByteBuffer = bufferPool.createWithDataWrapper(framePayloadData);
		frame.setErrorCode(Http2ErrorCode.fromInteger(payloadByteBuffer.getInt()));

		bufferPool.releaseBuffer(payloadByteBuffer);

		return null;
	}

}