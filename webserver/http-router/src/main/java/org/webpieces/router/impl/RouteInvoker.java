package org.webpieces.router.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webpieces.router.api.ResponseStreamer;
import org.webpieces.router.api.actions.Redirect;
import org.webpieces.router.api.actions.RenderHtml;
import org.webpieces.router.api.dto.HttpMethod;
import org.webpieces.router.api.dto.RedirectResponse;
import org.webpieces.router.api.dto.RenderResponse;
import org.webpieces.router.api.dto.RouterRequest;
import org.webpieces.router.api.dto.View;
import org.webpieces.router.api.exceptions.IllegalReturnValueException;
import org.webpieces.router.api.exceptions.NotFoundException;
import org.webpieces.router.api.routing.RouteId;
import org.webpieces.router.impl.params.ArgumentTranslator;

public class RouteInvoker {

	private static final Logger log = LoggerFactory.getLogger(RouteInvoker.class);
	private ArgumentTranslator argumentTranslator;
	//initialized in init() method and re-initialized in dev mode from that same method..
	private ReverseRoutes reverseRoutes;
	
	@Inject
	public RouteInvoker(ArgumentTranslator argumentTranslator) {
		this.argumentTranslator = argumentTranslator;
	}

	public void invoke(
			MatchResult result, RouterRequest req, ResponseStreamer responseCb, ErrorRoutes errorRoutes) {
		//We convert all exceptions from invokeAsync into CompletableFuture..
		CompletableFuture<Object> future = invokeAsync(result, req, responseCb, errorRoutes);
		future.exceptionally(e -> processException(responseCb, req, e, errorRoutes));
	}
	
	private Object processException(ResponseStreamer responseCb, RouterRequest req, Throwable e, ErrorRoutes errorRoutes) {
		if(e instanceof CompletionException) {
			//unwrap the exception to deliver the 'real' exception that occurred
			e = e.getCause();
		}
		
		if(e == null || e instanceof NotFoundException) {
			NotFoundException exc = (NotFoundException) e;
			//http 404...(unless an exception happens in calling this code and that then goes to 500)
			CompletableFuture<Object> future = notFound(errorRoutes, exc, req, responseCb);
			//If not found fails with sync or async exception, we processException and wrap in new Runtime to process as 500 next
			future.exceptionally(exception -> processException(responseCb, req, new RuntimeException("notFound page failed", exception), errorRoutes));
			return null;
		}

		//If this fails, then the users 5xx page is messed up and we then render our own 5xx page
		CompletableFuture<Object> future = internalServerError(errorRoutes, e, req, responseCb);
		future.exceptionally(finalExc -> finalFailure(responseCb, finalExc));
		
		return null;
	}
	
	public Object finalFailure(ResponseStreamer responseCb, Throwable e) {
		responseCb.failure(e);
		return null;
	}
	
	public CompletableFuture<Object> invokeAsync(
		MatchResult result, RouterRequest req, ResponseStreamer responseCb, ErrorRoutes notFoundRoute) {
		try {
			//This makes us consistent with other NotFoundExceptions and without the cost of 
			//throwing an exception and filling in stack trace...
			//We could convert the exc. to FastException and override method so stack is not filled in but that
			//can get very annoying
			if(result.getMeta().getRoute().isNotFoundRoute()) {
				processException(responseCb, req, null, notFoundRoute);
			}

			return invokeImpl(result, req, responseCb);
		} catch (Throwable e) {
			//http 500...
			//return a completed future with the exception inside...
			CompletableFuture<Object> futExc = new CompletableFuture<Object>();
			futExc.completeExceptionally(e);
			return futExc;
		}
	}
	
	private CompletableFuture<Object> notFound(ErrorRoutes errorRoutes, NotFoundException exc, RouterRequest req, ResponseStreamer responseCb) {
		try {
			MatchResult notFoundResult = errorRoutes.fetchNotfoundRoute(exc);
			return invokeImpl(notFoundResult, req, responseCb);
		} catch(Throwable e) {
			//http 500...
			//return a completed future with the exception inside...
			CompletableFuture<Object> futExc = new CompletableFuture<Object>();
			futExc.completeExceptionally(e);
			return futExc;			
		}
	}

	private CompletableFuture<Object> internalServerError(ErrorRoutes errorRoutes, Throwable exc, RouterRequest req, ResponseStreamer responseCb) {
		try {
			log.error("Exception occurred rendeering previous page.  Next try to render apps 5xx page", exc);
			MatchResult result = errorRoutes.fetchInternalServerErrorRoute();
			return invokeImpl(result, req, responseCb);
		} catch(Throwable e) {
			//http 500...
			//return a completed future with the exception inside...
			CompletableFuture<Object> futExc = new CompletableFuture<Object>();
			futExc.completeExceptionally(e);
			return futExc;			
		}
	}
	
	public CompletableFuture<Object> invokeImpl(MatchResult result, RouterRequest req, ResponseStreamer responseCb) {
		RouteMeta meta = result.getMeta();
		Object obj = meta.getControllerInstance();
		if(obj == null)
			throw new IllegalStateException("Someone screwed up, as controllerInstance should not be null at this point, bug");
		Method method = meta.getMethod();

		Object[] arguments = argumentTranslator.createArgs(result, req);
		//TODO: We need to render a page that says "This would be a 404 in production but in development this is an error as we assume you type
			//in the correct urls.  Something about your url matched a route but then failed.  Details are below
			//THEN we need tests in prod version and development version for this!!
		
		CompletableFuture<Object> response = invokeMethod(obj, method, arguments);
		
		RouteMeta finalMeta = meta;
		CompletableFuture<Object> future = response.thenApply(o -> continueProcessing(reverseRoutes, req, finalMeta, o, responseCb));
		return future;
	}

	public Object continueProcessing(ReverseRoutes reverseRoutes, RouterRequest routerRequest, RouteMeta incomingRequestMeta, Object controllerResponse, ResponseStreamer responseCb) {
		if(controllerResponse instanceof Redirect) {
			RedirectResponse httpResponse = processRedirect(reverseRoutes, routerRequest, incomingRequestMeta, (Redirect)controllerResponse);
			responseCb.sendRedirect(httpResponse);
		} else if(controllerResponse instanceof RenderHtml) {
			RenderResponse resp = renderHtml(routerRequest, incomingRequestMeta, (RenderHtml)controllerResponse);
			responseCb.sendRenderHtml(resp);
		} else {
			throw new UnsupportedOperationException("Not yet done but could "
					+ "call into the Action witht the responseCb to handle so apps can decide what to send back");
		}
		return null;
	}

	private RenderResponse renderHtml(RouterRequest routerRequest, RouteMeta incomingRequestMeta, RenderHtml controllerResponse) {
		Method method = incomingRequestMeta.getMethod();
		//in the case where the POST route was found, the controller canNOT be returning RenderHtml and should follow PRG
		//If the POST route was not found, just render the notFound page that controller sends us violating the
		//PRG pattern in this one specific case for now (until we test it with the browser to make sure back button is
		//not broken)
		if(!incomingRequestMeta.isNotFoundRoute() && HttpMethod.POST == routerRequest.method) {
			throw new IllegalReturnValueException("Controller method='"+method+"' MUST follow the PRG "
					+ "pattern(https://en.wikipedia.org/wiki/Post/Redirect/Get) so "
					+ "users don't have a poor experience using your website with the browser back button.  "
					+ "This means on a POST request, you cannot return RenderHtml object and must return Redirects");
		}
		
		View view = controllerResponse.getView();
		if(controllerResponse.getView() == null) {
			String controllerName = incomingRequestMeta.getControllerInstance().getClass().getName();
			String methodName = incomingRequestMeta.getMethod().getName();
			view = new View(controllerName, methodName);
		}
		
		RenderResponse resp = new RenderResponse(view, controllerResponse.getPageArgs(), incomingRequestMeta.getRoute().isNotFoundRoute());
		return resp;
	}

	private RedirectResponse processRedirect(ReverseRoutes reverseRoutes, RouterRequest r, RouteMeta incomingRequestMeta, Redirect action) {
		Method method = incomingRequestMeta.getMethod();
		RouteId id = action.getId();
		RouteMeta nextRequestMeta = reverseRoutes.get(id);
		
		if(nextRequestMeta == null)
			throw new IllegalReturnValueException("Route="+id+" returned from method='"+method+"' was not added in the RouterModules");

		Route route = nextRequestMeta.getRoute();
		
		Map<String, String> keysToValues = formMap(method, route.getPathParamNames(), action.getArgs());
		
		Set<String> keySet = keysToValues.keySet();
		List<String> argNames = route.getPathParamNames();
		if(keySet.size() != argNames.size()) {
			throw new IllegalReturnValueException("Method='"+method+"' returns a Redirect action with wrong number of arguments.  args="+keySet.size()+" when it should be size="+argNames.size());
		}

		String path = route.getPath();
		
		for(String name : argNames) {
			String value = keysToValues.get(name);
			if(value == null) 
				throw new IllegalArgumentException("Method='"+method+"' returns a Redirect that is missing argument key="+name+" to form the url on the redirect");
			path = path.replace("{"+name+"}", value);
		}
		
		return new RedirectResponse(r.isHttps, r.domain, path);
	}
	

	private Map<String, String> formMap(Method method, List<String> pathParamNames, List<Object> args) {
		if(pathParamNames.size() != args.size())
			throw new IllegalReturnValueException("The Redirect object returned from method='"+method+"' has the wrong number of arguments. args.size="+args.size()+" should be size="+pathParamNames.size());

		Map<String, String> nameToValue = new HashMap<>();
		for(int i = 0; i < pathParamNames.size(); i++) {
			String key = pathParamNames.get(i);
			Object obj = args.get(i);
			if(obj != null) {
				//TODO: need reverse binding here!!!!
				//Anotherwords, apps register Converters String -> Object and Object to String and we should really be
				//using that instead of toString to convert which could be different
				nameToValue.put(key, obj.toString());
			}
		}
		return nameToValue;
	}
	
	private CompletableFuture<Object> invokeMethod(Object obj, Method m, Object[] arguments) {
		try {
			return invokeMethodImpl(obj, m, arguments);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new InvokeException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private CompletableFuture<Object> invokeMethodImpl(Object obj, Method m, Object[] arguments) 
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Object retVal = m.invoke(obj, arguments);
		if(retVal instanceof CompletableFuture) {
			return (CompletableFuture) retVal;
		} else {
			return CompletableFuture.completedFuture(retVal);
		}
	}

	public void init(ReverseRoutes reverseRoutes) {
		this.reverseRoutes = reverseRoutes;
	}
}