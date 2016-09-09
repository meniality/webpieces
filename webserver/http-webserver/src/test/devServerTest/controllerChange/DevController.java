package org.webpieces.webserver.dev.app;

import org.webpieces.router.api.actions.Action;
import org.webpieces.router.api.actions.Actions;

public class DevController {

	public Action home() {
		String user = "CoolJeff";
		return Actions.renderThis("user", user);
	}

	public Action existingRoute() {
		return Actions.renderThis();
	}
	
	public Action notFound() {
		return Actions.renderThis();
	}
	
	public Action internalError() {
		return Actions.renderThis();
	}
}
