package org.webpieces.router.impl.loader;

import org.webpieces.router.api.extensions.BodyContentBinder;

public class LoadedContentController {

	private final LoadedController loadedController;
	private final BodyContentBinder binder;

	public LoadedContentController(LoadedController loadedController, BodyContentBinder binder) {
		this.loadedController = loadedController;
		this.binder = binder;
	}

	public LoadedController getLoadedController() {
		return loadedController;
	}

	public BodyContentBinder getBinder() {
		return binder;
	}
}
