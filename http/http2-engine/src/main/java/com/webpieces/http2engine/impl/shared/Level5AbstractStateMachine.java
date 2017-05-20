package com.webpieces.http2engine.impl.shared;

import java.util.concurrent.CompletableFuture;

import org.webpieces.javasm.api.Memento;
import org.webpieces.javasm.api.NoTransitionListener;
import org.webpieces.javasm.api.State;
import org.webpieces.javasm.api.StateMachine;
import org.webpieces.javasm.api.StateMachineFactory;
import org.webpieces.util.logging.Logger;
import org.webpieces.util.logging.LoggerFactory;

import com.webpieces.hpack.api.dto.Http2Headers;
import com.webpieces.hpack.api.dto.Http2Push;
import com.webpieces.http2engine.impl.shared.Http2Event.Http2SendRecieve;
import com.webpieces.http2parser.api.ConnectionException;
import com.webpieces.http2parser.api.ParseFailReason;
import com.webpieces.http2parser.api.StreamException;
import com.webpieces.http2parser.api.dto.DataFrame;
import com.webpieces.http2parser.api.dto.RstStreamFrame;
import com.webpieces.http2parser.api.dto.lib.PartialStream;

public abstract class Level5AbstractStateMachine {

	private static final Logger log = LoggerFactory.getLogger(Level5AbstractStateMachine.class);

	private Level6RemoteFlowControl remoteFlowControl;
	private Level6LocalFlowControl localFlowControl;

	protected StateMachine stateMachine;
	protected State idleState;
	protected State closed;
	protected State openState;

	public Level5AbstractStateMachine(String id, Level6RemoteFlowControl remoteFlowControl, Level6LocalFlowControl localFlowControl) {
		this.remoteFlowControl = remoteFlowControl;
		this.localFlowControl = localFlowControl;
		
		StateMachineFactory factory = StateMachineFactory.createFactory();
		stateMachine = factory.createStateMachine(id);

		idleState = stateMachine.createState("idle");
		openState = stateMachine.createState("Open");
		closed = stateMachine.createState("closed");
	}

	public CompletableFuture<Void> fireToSocket(Stream stream, PartialStream payload) {
		Memento state = stream.getCurrentState();
		Http2Event event = translate(Http2SendRecieve.SEND, payload);

		log.info("state before event="+state.getCurrentState()+" event="+event);
		State result = stateMachine.fireEvent(state, event);
		
		log.info("state after="+result);
		//if no exceptions occurred, send it on to flow control layer
		return remoteFlowControl.sendPayloadToSocket(stream, payload);
	}
	
	public CompletableFuture<State> fireToClient(Stream stream, PartialStream payload, Runnable possiblyClose) {
		Memento currentState = stream.getCurrentState();
		Http2Event event = translate(Http2SendRecieve.RECEIVE, payload);
		
		try {
			return fireToClientImpl(stream, payload, possiblyClose, currentState, event);
		} catch(NoTransitionConnectionError t) {
			throw new ConnectionException(ParseFailReason.BAD_FRAME_RECEIVED_FOR_THIS_STATE, stream.getStreamId(), t.getMessage(), t);
		} catch(NoTransitionStreamError t) {
			throw new StreamException(ParseFailReason.CLOSED_STREAM, stream.getStreamId(), t.getMessage(), t);				
		}
	}

	private CompletableFuture<State> fireToClientImpl(Stream stream, PartialStream payload, Runnable possiblyClose,
			Memento currentState, Http2Event event) {
		log.info("firing event to new statemachine="+event+" state="+currentState.getCurrentState());
		State result = stateMachine.fireEvent(currentState, event);
		log.info("done firing.  new state="+result);
		//closing the stream should be done BEFORE firing to client as if the stream is closed
		//then this will prevent windowUpdateFrame with increment being sent to a closed stream
		if(possiblyClose != null)
			possiblyClose.run();

		return localFlowControl.fireToClient(stream, payload)
			.thenApply( v -> result);
	}
	
	public boolean isInClosedState(Stream stream) {
		State currentState = stream.getCurrentState().getCurrentState();
		if(currentState == closed)
			return true;
		return false;
	}
	
	public Memento createStateMachine(String streamId) {
		return stateMachine.createMementoFromState("stream"+streamId, idleState);
	}
	
	
	protected Http2Event translate(Http2SendRecieve sendRecv, PartialStream payload) {
		Http2PayloadType payloadType;
		if(payload instanceof Http2Headers) {
			if(payload.isEndOfStream())
				payloadType = Http2PayloadType.HEADERS_WITH_EOS;
			else
				payloadType = Http2PayloadType.HEADERS;
		} else if(payload instanceof DataFrame) {
			if(payload.isEndOfStream())
				payloadType = Http2PayloadType.DATA_WITH_EOS;
			else
				payloadType = Http2PayloadType.DATA;
		} else if(payload instanceof Http2Push) {
			payloadType = Http2PayloadType.PUSH_PROMISE;
		} else if(payload instanceof RstStreamFrame) {
			payloadType = Http2PayloadType.RESET_STREAM;
		} else
			throw new IllegalArgumentException("unknown payload type for payload="+payload);
		
		return new Http2Event(sendRecv, payloadType);
	}
	

	protected static class NoTransitionImpl implements NoTransitionListener {
		private boolean isConnectionError;

		public NoTransitionImpl(boolean isConnectionError) {
			this.isConnectionError = isConnectionError;
		}

		@Override
		public void noTransitionFromEvent(State state, Object event) {
			if(isConnectionError)
				throw new NoTransitionConnectionError("No transition defined on statemachine for event="+event+" when in state="+state);
			else
				throw new NoTransitionStreamError("No transition defined on statemachine for event="+event+" when in state="+state);
		}
	}
}
