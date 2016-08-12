package org.webpieces.webserver.basic;

import static org.webpieces.router.api.dto.HttpMethod.*;
import org.webpieces.router.api.routing.AbstractRouteModule;

public class BasicRouteModule extends AbstractRouteModule {

	@Override
	protected void configure(String currentPackage) {
		addRoute(GET , "/",                  "biz/BasicController.someMethod", BasicRouteId.SOME_ROUTE);
		addRoute(GET , "/redirect/{id}",     "biz/BasicController.redirect", BasicRouteId.REDIRECT_PAGE);
		addRoute(GET , "/redirectint/{id}",  "biz/BasicController.redirectWithInt", BasicRouteId.REDIRECT2);
		addRoute(GET , "/myroute",           "biz/BasicController.myMethod", BasicRouteId.RENDER_PAGE);
		addRoute(GET , "/throwNotFound",     "biz/BasicController.throwNotFound", BasicRouteId.THROW_NOT_FOUND);
		addRoute(GET , "/pageparam",         "biz/BasicController.pageParam", BasicRouteId.PAGE_PARAM);

		addRoute(GET , "/urlencoding/{user}","biz/BasicController.urlEncoding", BasicRouteId.URLENCODE);
		// #{form action:@POST_CATCH_ALL[:] id:'detailsForm'}#  ....   #{/form}#
		//addPostRoute("/post/{_controller}/{_action}", "{_controller}.post{_action}", BasicRouteId.POST_CATCH_ALL); //catch all post route
		
		setPageNotFoundRoute("biz/BasicController.notFound");
		setInternalErrorRoute("biz/BasicController.internalError");
	}

}
