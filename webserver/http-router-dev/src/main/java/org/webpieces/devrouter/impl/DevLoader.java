package org.webpieces.devrouter.impl;

import javax.inject.Inject;

import org.webpieces.router.api.routing.RouteFilter;
import org.webpieces.router.impl.FilterInfo;
import org.webpieces.router.impl.RouteMeta;
import org.webpieces.router.impl.hooks.MetaLoaderProxy;
import org.webpieces.router.impl.loader.AbstractLoader;
import org.webpieces.router.impl.loader.MetaLoader;
import org.webpieces.router.impl.loader.ResolvedMethod;

import com.google.inject.Injector;

public class DevLoader extends AbstractLoader implements MetaLoaderProxy {

	private DevClassForName classLoader;

	@Inject
	public DevLoader(MetaLoader loader, DevClassForName classLoader) {
		super(loader);
		this.classLoader = classLoader;
	}
	
	protected Object createController(Injector injector, String controllerClassFullName) {
		Class<?> clazz = classLoader.clazzForName(controllerClassFullName);
		return injector.getInstance(clazz);
	}

	@SuppressWarnings("unchecked")
	protected <T> RouteFilter<T> createFilterImpl(Injector injector, FilterInfo<T> info) {
		Class<? extends RouteFilter<T>> filterClass = info.getFilter();
		//Must get the string as the Class here will be WRONG if it got recompiled as it will be a new instance
		//coming from a different classloader
		Class<? extends RouteFilter<T>> latestFilterClass = (Class<? extends RouteFilter<T>>) classLoader.clazzForName(filterClass.getName());
		return injector.getInstance(latestFilterClass);
	}
	
	@Override
	public void loadControllerIntoMeta(RouteMeta meta, ResolvedMethod method,
			boolean isInitializingAllControllers) {
		if(isInitializingAllControllers)
			return; //skip on startup
		
		super.loadRouteImpl(meta, method);
	}

	@Override
	public void loadFiltersIntoMeta(RouteMeta m, boolean isInitializingAllFilters) {
		if(isInitializingAllFilters)
			return; //skip on startup
		
		super.loadFiltersIntoMeta(m);
	}
}
