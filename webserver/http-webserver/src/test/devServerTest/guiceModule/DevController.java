package org.webpieces.webserver.dev.app;

import javax.inject.Inject;

import org.webpieces.router.api.actions.Action;
import org.webpieces.router.api.actions.Actions;

public class DevController {

	@Inject
	private NewInterface library;
	
	public Action home() {
		String user = library.fetchName();
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
