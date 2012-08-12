package org.swingexplorer.intellij.plugin;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.util.IconLoader;

/**
 * 
 * @author Piotr Mlocek
 */
public class Executor extends DefaultRunExecutor {
	@NotNull
	@Override
	public String getId() {
		return SwingExplorerPlugin.RUNNER_ID;
	}

	@Override
	public String getContextActionId() {
		return SwingExplorerPlugin.EXECUTOR_CONTEXT_ACTION_ID;
	}

	@NotNull
	@Override
	public Icon getIcon() {
		return IconLoader.getIcon("/execute18x18.png");
	}
}
