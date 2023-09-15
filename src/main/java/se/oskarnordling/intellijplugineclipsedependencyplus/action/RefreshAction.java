package se.oskarnordling.intellijplugineclipsedependencyplus.action;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Oskar Nordling
 */
public class RefreshAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AddAction.handle(e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE),
                         e.getDataContext().getData(PlatformDataKeys.MODULE),
                         e.getDataContext().getData(PlatformDataKeys.PROJECT));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        Module module = e.getDataContext().getData(PlatformDataKeys.MODULE);
        Project project = e.getDataContext().getData(PlatformDataKeys.PROJECT);
        if (file != null && module != null && project != null && file.getName().equalsIgnoreCase(".classpath")) {
            e.getPresentation().setVisible(AddAction.libraryExists(module, project));
        } else {
            e.getPresentation().setVisible(false);
        }
    }
}
