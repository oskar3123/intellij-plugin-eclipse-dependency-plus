package se.oskarnordling.intellijplugineclipsedependencyplus.action;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Oskar Nordling
 */
public class Group extends DefaultActionGroup {

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        if (file != null && file.getName().equalsIgnoreCase(".classpath")) {
            e.getPresentation().setVisible(true);
        } else {
            e.getPresentation().setVisible(false);
        }
    }
}
