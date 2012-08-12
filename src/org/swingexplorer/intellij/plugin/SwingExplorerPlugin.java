package org.swingexplorer.intellij.plugin;

import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;

/**
 * 
 * @author Piotr Mlocek
 */
public class SwingExplorerPlugin implements PluginDescriptor {
	public static final String PLUGIN_ID = "swingexplorer-plugin";
	public static final String RUNNER_ID = "Run-with-SE";
	public static final String EXECUTOR_CONTEXT_ACTION_ID = "RunWithSEClass";

	@Override
	public PluginId getPluginId() {
		return PluginId.getId(PLUGIN_ID);
	}

	@Override
	public ClassLoader getPluginClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}
}
