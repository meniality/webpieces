package org.webpieces.plugins.backend;

public class BackendConfig {

	private boolean isUsePluginAssets;

	public BackendConfig(boolean isUsePluginAssets) {
		super();
		this.isUsePluginAssets = isUsePluginAssets;
	}

	public boolean isUsePluginAssets() {
		return isUsePluginAssets;
	}

}
