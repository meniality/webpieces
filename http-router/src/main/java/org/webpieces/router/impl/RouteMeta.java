package org.webpieces.router.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.webpieces.router.api.dto.RouterRequest;

import com.google.inject.Injector;

public class RouteMeta {

	private final Route route;
	private Object controllerInstance;
	private Method method;
	private List<String> methodParamNames;
	private boolean isNotFoundRoute;
	//The package for the RouteModule for context(so controllers are relative to that module)
	private String packageContext;
	private Injector injector;

	public RouteMeta(Route r, Injector injector, String packageContext, boolean isNotFoundRoute) {
		this.route = r;
		this.isNotFoundRoute = isNotFoundRoute;
		this.packageContext = packageContext;
		this.injector = injector;
	}

	public Route getRoute() {
		return route;
	}

	public Object getControllerInstance() {
		return controllerInstance;
	}

	public Method getMethod() {
		return method;
	}

	public void setControllerInstance(Object controllerInstance) {
		this.controllerInstance = controllerInstance;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public void setMethodParamNames(List<String> paramNames) {
		this.methodParamNames = paramNames;
	}

	public MatchResult matches(RouterRequest request, String path) {
		Matcher matcher = route.matches(request, path);
		if(matcher == null)
			return null;
		else if(!matcher.matches())
			return null;
		
		
		List<String> names = route.getPathParamNames();

		Map<String, String> namesToValues = new HashMap<>();
		for(String name : names) {
			String value = matcher.group(name);
			if(value == null) 
				throw new IllegalArgumentException("Bug, something went wrong. request="+request);
			namesToValues.put(name, value);
		}
		
		return new MatchResult(this, namesToValues);
	}
	
	@Override
	public String toString() {
		return "\nRouteMeta [route=\n   " + route + ", \n   method=" + method
				+ ", methodParamNames=" + methodParamNames + "]";
	}

	public boolean isNotFoundRoute() {
		return isNotFoundRoute;
	}

	public String getPackageContext() {
		return packageContext;
	}

	public Injector getInjector() {
		return injector;
	}
	
}
