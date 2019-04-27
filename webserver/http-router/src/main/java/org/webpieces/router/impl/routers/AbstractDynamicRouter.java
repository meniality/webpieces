package org.webpieces.router.impl.routers;

import org.webpieces.router.impl.routebldr.BaseRouteInfo;

public interface AbstractDynamicRouter extends AbstractRouter {

	void setBaseRouteInfo(BaseRouteInfo baseRouteInfo);

	void setDynamicInfo(DynamicInfo dynamicInfo);

}