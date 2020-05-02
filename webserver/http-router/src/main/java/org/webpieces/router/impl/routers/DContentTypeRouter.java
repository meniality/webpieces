package org.webpieces.router.impl.routers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.webpieces.ctx.api.RequestContext;
import org.webpieces.router.api.ResponseStreamer;
import org.webpieces.router.api.exceptions.NotFoundException;
import org.webpieces.router.impl.model.MatchResult2;
import org.webpieces.util.futures.ExceptionUtil;

public class DContentTypeRouter {

	private List<AbstractRouter> routers;

	public DContentTypeRouter(List<AbstractRouter> routers) {
		this.routers = routers;
	}

	public String build(String spacing) {
		String text = "\n";
		
		for(AbstractRouter route: routers) {
			text += spacing+route.getMatchInfo().getLoggableString(" ")+"\n";
		}
		
		text+="\n";
		
		return text;
	}

	public CompletableFuture<Void> invokeRoute(RequestContext ctx, ResponseStreamer responseCb, String relativePath) {
		for(AbstractRouter router : routers) {
			MatchResult2 result = router.matches(ctx.getRequest(), relativePath);
			if(result.isMatches()) {
				ctx.setPathParams(result.getPathParams());
				
				return router.invoke(ctx, responseCb);
			}
		}

		return ExceptionUtil.<Void>failedFuture(new NotFoundException("route not found"));
	}

}