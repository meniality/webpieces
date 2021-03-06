package com.webpieces.http2parser.impl.marshallers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;

import org.webpieces.data.api.BufferPool;
import org.webpieces.data.api.DataWrapper;
import org.webpieces.data.api.DataWrapperGenerator;

import com.webpieces.http2.api.dto.error.CancelReasonCode;
import com.webpieces.http2.api.dto.error.ConnectionException;
import com.webpieces.http2.api.dto.lowlevel.SettingsFrame;
import com.webpieces.http2.api.dto.lowlevel.lib.AbstractHttp2Frame;
import com.webpieces.http2.api.dto.lowlevel.lib.Http2Frame;
import com.webpieces.http2.api.dto.lowlevel.lib.Http2Setting;
import com.webpieces.http2.api.dto.lowlevel.lib.SettingsParameter;
import com.webpieces.http2parser.impl.FrameHeaderData;
import com.webpieces.http2parser.impl.Http2MementoImpl;
import com.webpieces.http2parser.impl.UnsignedData;

public class SettingsMarshaller extends AbstractFrameMarshaller implements FrameMarshaller {

	public SettingsMarshaller(BufferPool bufferPool, DataWrapperGenerator dataGen) {
		super(bufferPool);
	}

	@Override
	public DataWrapper marshal(Http2Frame frame) {
    	if(frame.getStreamId() != 0)
    		throw new IllegalArgumentException("SettingsFrame can never be any other stream id except 0 which is already set");
    	
		SettingsFrame castFrame = (SettingsFrame) frame;
		for(Http2Setting setting : castFrame.getSettings()) {
			validate(setting);
		}

		byte flags = 0x0;
		if (castFrame.isAck())
			flags |= 0x1;

		DataWrapper dataPayload;
		if (castFrame.isAck()) {
			if(castFrame.getSettings() != null && castFrame.getSettings().size() > 0)
	    		throw new IllegalArgumentException("Ack SettingsFrame can not have setting in it");

			dataPayload = DATA_GEN.emptyWrapper();
		} else if(castFrame.getSettings().size() == 0) {
			dataPayload = DATA_GEN.emptyWrapper();
		} else {
			List<Http2Setting> settings = castFrame.getSettings();
			dataPayload = marshalOut(settings);
		}
		return super.marshalFrame(frame, flags, dataPayload);
	}

	@Override
	public AbstractHttp2Frame unmarshal(Http2MementoImpl state, DataWrapper payload) {
		FrameHeaderData frameHeaderData = state.getFrameHeaderData();
		int payloadLength = frameHeaderData.getPayloadLength();
		int streamId = frameHeaderData.getStreamId();
        
		SettingsFrame frame = new SettingsFrame();
		super.unmarshalFrame(state, frame);

		byte flags = state.getFrameHeaderData().getFlagsByte();
		frame.setAck((flags & 0x1) == 0x1);

		if(frame.isAck()) {
	        if(payloadLength != 0) {
	            throw new ConnectionException(CancelReasonCode.FRAME_SIZE_INCORRECT, streamId, 
	            		"size of payload of a settings frame ack must be 0 but was="+payloadLength);	        }
		} else if(payloadLength % 6 != 0) {
            throw new ConnectionException(CancelReasonCode.FRAME_SIZE_INCORRECT, streamId, 
            		"payload size must be a multiple of 6 but was="+state.getFrameHeaderData().getPayloadLength());
        } else if(streamId != 0)
            throw new ConnectionException(CancelReasonCode.INVALID_STREAM_ID, streamId, 
            		"settings frame had stream id="+streamId);
        
		ByteBuffer payloadByteBuffer = bufferPool.createWithDataWrapper(payload);

		List<Http2Setting> settingsList = unmarshal(payloadByteBuffer);
		frame.setSettings(settingsList);

		bufferPool.releaseBuffer(payloadByteBuffer);

		return frame;
	}

	private List<Http2Setting> unmarshal(ByteBuffer payloadByteBuffer) {
		List<Http2Setting> settings = new ArrayList<>();
		while (payloadByteBuffer.hasRemaining()) {
			int id = UnsignedData.getUnsignedShort(payloadByteBuffer);
			long value = UnsignedData.getUnsignedInt(payloadByteBuffer);
			Http2Setting http2Setting = new Http2Setting(id, value);
			settings.add(http2Setting);
			validate(http2Setting);
		}
		return settings;
	}

	private void validate(Http2Setting http2Setting) {
		SettingsParameter key = SettingsParameter.lookup(http2Setting.getId());
		long value = http2Setting.getValue();
		if(key == null)
			return; //unknown setting
		
		switch(key) {
			case SETTINGS_ENABLE_PUSH:
				if(value != 0 && value != 1)
		            throw new ConnectionException(CancelReasonCode.INVALID_SETTING, 0, 
		            		"push setting must be 0 or 1 but was="+value);
				break;
			case SETTINGS_INITIAL_WINDOW_SIZE:
				validateWindowSize(value);
				break;
			case SETTINGS_MAX_FRAME_SIZE:
				validateMaxFrameSize(value);
				break;
			case SETTINGS_HEADER_TABLE_SIZE:
			case SETTINGS_MAX_CONCURRENT_STREAMS:
			case SETTINGS_MAX_HEADER_LIST_SIZE:
				break;
			default:
				throw new IllegalArgumentException("case statement missing new setting="+key+" with value="+value);
		}
	}

	private void validateWindowSize(long value) {
        // 2^31 - 1 - max flow control window
		int min = 0;
		int max = 2147483647;
		
		if(value < min || value > max)
            throw new ConnectionException(CancelReasonCode.SETTINGS_WINDOW_SIZE_INVALID, 0, 
            		"window size must be between "+min+" and "+max+" but was="+value);
	}
	
	private void validateMaxFrameSize(long value) {
        // frame size must be between 16384 and 2^24 - 1
		int min = 16384;
		int max = 1677215;
		
		if(value < min || value > max)
            throw new ConnectionException(CancelReasonCode.INVALID_SETTING, 0, 
            		"window size must be between "+min+" and "+max+" but was="+value);
	}

	public List<Http2Setting> unmarshalPayload(String base64SettingsPayload) {
		Decoder decoder = Base64.getDecoder();
		byte[] decoded = decoder.decode(base64SettingsPayload);
		ByteBuffer buf = ByteBuffer.wrap(decoded);
		return unmarshal(buf);
	}

	public String marshalPayload(List<Http2Setting> settingsPayload) {
		DataWrapper data = marshalOut(settingsPayload);
		byte[] byteBuf = data.createByteArray();
		Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(byteBuf);
	}
	
	private DataWrapper marshalOut(List<Http2Setting> settings) {
		DataWrapper dataPayload;
		ByteBuffer payload = bufferPool.nextBuffer(6 * settings.size());

		for (Http2Setting setting : settings) {
			UnsignedData.putUnsignedShort(payload, setting.getId());
			UnsignedData.putUnsignedInt(payload, setting.getValue());
		}
		payload.flip();

		dataPayload = DATA_GEN.wrapByteBuffer(payload);
		return dataPayload;
	}
}