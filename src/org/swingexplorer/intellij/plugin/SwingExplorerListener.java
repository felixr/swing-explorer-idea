package org.swingexplorer.intellij.plugin;

import java.util.HashMap;

import javax.management.Notification;
import javax.management.NotificationListener;

import org.jetbrains.annotations.NotNull;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * 
 * @author Piotr Mlocek
 */
public class SwingExplorerListener implements NotificationListener {
	private static final String EVENT_LOG_TOOL_WINDOW_NAME = "Event Log";
	private final Project project;
	private final ToolWindowManager toolWindowManager;

	public SwingExplorerListener(Project project) {
		this.project = project;
		toolWindowManager = ToolWindowManager.getInstance(project);
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		@SuppressWarnings({ "rawtypes" })
		HashMap data = (HashMap) notification.getUserData();
		final String className = (String) data.get("className");
		final int lineNumber = (Integer) data.get("lineNumber");

		new SourceNavigator(className, lineNumber).run();
	}

	private class SourceNavigator implements Runnable {
		private final String className;
		private final int lineNumber;

		public SourceNavigator(String className, int lineNumber) {
			this.className = className;
			this.lineNumber = lineNumber;
		}

		@Override
		public void run() {
			final Application application = ApplicationManager.getApplication();
			application.invokeLater(new Runnable() {
				public void run() {
					application.runReadAction(new Runnable() {
						public void run() {
							if (project.isInitialized()) {
								activateFile();
							}
						}
					});
				}
			});
		}

		private void activateFile() {
			final GlobalSearchScope searchScope = GlobalSearchScope
					.allScope(project);
			PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(
					className, searchScope);
			if (psiClass != null) {
				navigateToClass(psiClass);
				removeErrorBubble();
			} else {
				showWarningBubble();
			}
		}

		private void removeErrorBubble() {
			Balloon toolWindowBalloon = toolWindowManager
					.getToolWindowBalloon(EVENT_LOG_TOOL_WINDOW_NAME);
			if (toolWindowBalloon != null)
				toolWindowBalloon.hide();
		}

		private void showWarningBubble() {
			String message = "Could not find class " + className;

			Notifications.Bus
					.notify(new com.intellij.notification.Notification(
							"Swing Explorer", "Class not found", message,
							NotificationType.WARNING));
			toolWindowManager.notifyByBalloon(EVENT_LOG_TOOL_WINDOW_NAME,
					MessageType.WARNING, message);
		}

		private boolean navigateToClass(PsiClass psiClass) {
			FileEditorProviderManager editorProviderManager = FileEditorProviderManager
					.getInstance();
			VirtualFile virtualFile = psiClass.getContainingFile()
					.getVirtualFile();

			if (virtualFile == null
					|| editorProviderManager.getProviders(project, virtualFile).length == 0) {
				return false;
			}
			OpenFileDescriptor descriptor = new OpenFileDescriptor(project,
					virtualFile);

			final Editor editor = FileEditorManager.getInstance(project)
					.openTextEditor(descriptor, true);
			if (editor != null) {
				if (lineNumber > editor.getDocument().getLineCount()) {
					return false;
				}
				CaretModel caretModel = editor.getCaretModel();
				LogicalPosition pos = new LogicalPosition(lineNumber - 1, 0);
				caretModel.moveToLogicalPosition(pos);

				ApplicationManager.getApplication().invokeLater(new Runnable() {
					public void run() {
						editor.getScrollingModel().scrollToCaret(
								ScrollType.CENTER);
					}
				});
			}
			activateProjectWindow(project);
			return true;
		}
	}

	private static void activateProjectWindow(@NotNull Project project) {
		ProjectUtil.focusProjectWindow(project, true);
	}
}