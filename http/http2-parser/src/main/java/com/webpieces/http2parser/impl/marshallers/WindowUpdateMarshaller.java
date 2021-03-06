package com.webpieces.http2parser.impl.marshallers;

import java.nio.ByteBuffer;

import org.webpieces.data.api.BufferPool;
import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;

import com.webpieces.http2.api.dto.error.CancelReasonCode;
import com.webpieces.http2.api.dto.error.ConnectionException;
import com.webpieces.http2.api.dto.lowlevel.WindowUpdateFrame;
import com.webpieces.http2.api.dto.lowlevel.lib.AbstractHttp2Frame;
import com.webpieces.http2.api.dto.lowlevel.lib.Http2Frame;
import com.webpieces.http2parser.impl.FrameHeaderData;
import com.webpieces.http2parser.impl.Http2MementoImpl;

public class WindowUpdateMarshaller extends AbstractFrameMarshaller implements FrameMarshaller {

	public WindowUpdateMarshaller(BufferPool bufferPool, DataWrapperGenerator dataGen) {
		super(bufferPool);
	}

	@Override
	public DataWrapper marshal(Http2Frame frame) {
		WindowUpdateFrame castFrame = (WindowUpdateFrame) frame;
		ByteBuffer payload = bufferPool.nextBuffer(4).putInt(castFrame.getWindowSizeIncrement());
		payload.flip();

		DataWrapper dataPayload = DATA_GEN.wrapByteBuffer(payload);
		return super.marshalFrame(frame, (byte) 0, dataPayload);
	}

	@Override
	public AbstractHttp2Frame unmarshal(Http2MementoImpl state, DataWrapper payload) {
		FrameHeaderData frameHeaderData = state.getFrameHeaderData();
		int streamId = frameHeaderData.getStreamId();
		if(state.getFrameHeaderData().getPayloadLength() != 4)
            throw new ConnectionException(CancelReasonCode.FRAME_SIZE_INCORRECT, streamId, 
            		"window update size not 4 and instead is="+state.getFrameHeaderData().getPayloadLength());
		
		WindowUpdateFrame frame = new WindowUpdateFrame();
		super.unmarshalFrame(state, frame);

		ByteBuffer payloadByteBuffer = bufferPool.createWithDataWrapper(payload);

		frame.setWindowSizeIncrement(payloadByteBuffer.getInt());
		bufferPool.releaseBuffer(payloadByteBuffer);
		
		if(frame.getWindowSizeIncrement() == 0)
            throw new ConnectionException(CancelReasonCode.WINDOW_SIZE_INVALID, streamId, 
            		"Window size increment cannot be 0 per http/2 spec and was");
		
		return frame;
	}
}
