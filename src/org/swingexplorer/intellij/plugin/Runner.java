package org.swingexplorer.intellij.plugin;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.PathsList;

/**
 * 
 * @author Piotr Mlocek
 */
public class Runner extends DefaultProgramRunner {
	private VirtualFile swagJarFile;
	private VirtualFile swexplJarFile;
	private Project project;
	private int port;

	@NotNull
	@Override
	public String getRunnerId() {
		return SwingExplorerPlugin.EXECUTOR_CONTEXT_ACTION_ID;
	}

	@Nullable
	public static VirtualFile getPluginVirtualDirectory() {
		IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId
				.getId(SwingExplorerPlugin.PLUGIN_ID));
		if (descriptor != null) {
			File pluginPath = descriptor.getPath();
			String url = VfsUtil.pathToUrl(pluginPath.getAbsolutePath());

			return VirtualFileManager.getInstance().findFileByUrl(url);
		}
		return null;
	}

	@Override
	protected RunContentDescriptor doExecute(Project project,
			Executor executor, RunProfileState runProfileState,
			RunContentDescriptor runContentDescriptor,
			ExecutionEnvironment executionEnvironment)
			throws ExecutionException {
		this.project = project;

		FileDocumentManager.getInstance().saveAllDocuments();
		initJavaSettings(runProfileState);

		ExecutionResult executionResult = runProfileState.execute(executor,
				this);
		if (executionResult == null)
			return null;

		final RunContentBuilder contentBuilder = new RunContentBuilder(project,
				this, executor);
		contentBuilder.setExecutionResult(executionResult);
		contentBuilder.setEnvironment(executionEnvironment);

		initListener();

		return contentBuilder.showRunContent(runContentDescriptor);
	}

	private void initJavaSettings(RunProfileState runProfileState)
			throws ExecutionException {
		if (runProfileState instanceof ApplicationConfiguration.JavaApplicationCommandLineState) {
			ApplicationConfiguration.JavaApplicationCommandLineState profileState = (ApplicationConfiguration.JavaApplicationCommandLineState) runProfileState;

			initJarFiles();
			initPort();
			appendSwingExplorerJarsToClassPath(profileState);

			JavaParameters javaParameters = profileState.getJavaParameters();

			ParametersList vmParametersList = javaParameters
					.getVMParametersList();
			vmParametersList.add("-javaagent:" + swagJarFile.getPath());
			vmParametersList.add("-Xbootclasspath/a:" + swagJarFile.getPath());
			vmParametersList.add("-Dswex.mport=" + port);
			vmParametersList.add("-Dcom.sun.management.jmxremote");

			String mainClass = javaParameters.getMainClass();
			javaParameters.setMainClass("org.swingexplorer.Launcher");
			javaParameters.getProgramParametersList().addAt(0, mainClass);
		}
	}

	private void initPort() throws ExecutionException {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(0);
			port = serverSocket.getLocalPort();
			serverSocket.close();
		} catch (IOException e) {
			throw new ExecutionException("Could not open port!");
		}
	}

	private void initListener() throws ExecutionException {
		try {
			JMXServiceURL url = new JMXServiceURL(
					"service:jmx:rmi:///jndi/rmi://:" + port + "/server");
			JMXConnector jmxc = connectToSwingExplorer(url);
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

			ObjectName name = new ObjectName(
					"org.swingexplorer:name=IDESupport");
			mbsc.invoke(name, "connect", new Object[0], new String[0]);

			SwingExplorerListener swingExplorerListener = new SwingExplorerListener(
					project);
			mbsc.addNotificationListener(name, swingExplorerListener, null,
					null);
		} catch (Exception e) {
			throw new ExecutionException("Could not find free port!", e);
		}
	}

	private JMXConnector connectToSwingExplorer(JMXServiceURL url)
			throws InterruptedException, ExecutionException {
		Thread.sleep(2000);
		JMXConnector jmxc = null;

		int retries = 10;
		while (retries-- > 0) {
			try {
				jmxc = JMXConnectorFactory.connect(url, null);
				return jmxc;
			} catch (IOException e) {
				Thread.sleep(1000);
			}
		}
		throw new ExecutionException("Could not connect to SwingExplorer!");
	}

	private void appendSwingExplorerJarsToClassPath(
			ApplicationConfiguration.JavaApplicationCommandLineState profileState)
			throws ExecutionException {
		PathsList classPath = profileState.getJavaParameters().getClassPath();

		classPath.add(swagJarFile);
		classPath.add(swexplJarFile);
	}

	private void initJarFiles() throws ExecutionException {
		VirtualFile pluginDir = getPluginVirtualDirectory();
		boolean ok = false;

		if (pluginDir != null) {
			VirtualFile lib = pluginDir.findChild("lib");
			if (lib != null) {
				swagJarFile = lib.findChild("swag.jar");
				swexplJarFile = lib.findChild("swexpl.jar");

				ok = swagJarFile != null && swexplJarFile != null;
			}
		}
		if (!ok) {
			throw new ExecutionException(
					"SwingExplorer jars could not be found! " + pluginDir);
		}
	}

	@Override
	public boolean canRun(@NotNull String s, @NotNull RunProfile runProfile) {
		return s.equals(SwingExplorerPlugin.RUNNER_ID)
				&& (runProfile instanceof ApplicationConfiguration);
	}
}
