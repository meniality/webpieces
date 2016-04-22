package org.webpieces.util.futures;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class PromiseImpl<T, F> implements Future<T, F>, Promise<T, F> {

	private boolean complete = false;
	private Consumer<T> resultFunc;
	private Consumer<F> failureFunc;
	private T result;
	private F failure;
	private Consumer<String> cancelFunc;
	private String cancelReason;
	private Executor executor;
	private static ThreadLocal<Integer> counterThreadLocal = new ThreadLocal<>();
	
	public PromiseImpl(Executor executor) {
		this.executor = executor;
	}

	public void setResult(T result) {
		if(result == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.result = result;
		fire(resultFunc, result);
	}
	
	public void setFailure(F failure) {
		if(failure == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.failure = failure;
		fire(failureFunc, failure);
	}
	
	@Override
	public void cancel(String reason) {
		if(reason == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.cancelReason = reason;
		fire(cancelFunc, cancelReason);
	}
	
	public PromiseImpl<T,F> setResultFunction(Consumer<T> resultFunction) {
		if(resultFunction == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.resultFunc = resultFunction;
		fire(resultFunc, result);
		return this;
	}
	
	public PromiseImpl<T,F> setFailureFunction(Consumer<F> failureFunction) {
		if(failureFunction == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.failureFunc = failureFunction;
		fire(failureFunc, failure);
		return this;
	}

	@Override
	public Future<T, F> setCancelFunction(Consumer<String> cancelFunction) {
		if(cancelFunction == null)
			throw new IllegalArgumentException("param cannot be null(this operates off null checks)");
		this.cancelFunc = cancelFunction;
		fire(cancelFunc, cancelReason);
		return this;
	}

	private <E> void fire(Consumer<E> consumer, E result) {
		if(consumer == null || result == null)
			return;
		
		synchronized (this) {
			if(complete) //only allow completing once
				return;
			
			complete = true;
		}
		//the rest done outside synchronization block..especially notifying consumer
		
		//This is a special trick to keep this thread going but eventually we could get stackoverflow
		//so at the point of 100 times recursion, we offload to another thread
		int count = incrementThreadLocalCounterAndFetch();
		final E resultToUse = result;
		if(count >= 100) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					consumer.accept(resultToUse);
				}
			});
		} else {
			consumer.accept(result);
		}
		
		//This is very important.  The result must be nulled out.  If an Promise makes it to old gen
		//we dont' want it dragging a result that just occurred into old gen.
		//This is when a result comes in > 10 to 30 seconds after the promise was created but
		//the result is hopefully short lived while the promise lived until old gen.  The garbage
		//collector doesn't know the objects in old gen are not live until a stop the world gc
		result = null;
		
		//now we can reset the counter since we are no longer in the stack
		counterThreadLocal.set(null);
	}

	private int incrementThreadLocalCounterAndFetch() {
		Integer count = counterThreadLocal.get();
		if(count == null) {
			counterThreadLocal.set(1);
			return 1;
		}
		
		int newCount = count+1;
		counterThreadLocal.set(newCount);
		return newCount;
	}

	@Override
	public synchronized boolean isComplete() {
		return complete;
	}
	
}
