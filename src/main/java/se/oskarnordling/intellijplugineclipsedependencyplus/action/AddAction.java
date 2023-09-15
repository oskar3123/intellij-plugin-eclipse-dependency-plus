package se.oskarnordling.intellijplugineclipsedependencyplus.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.Library.ModifiableModel;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import se.oskarnordling.intellijplugineclipsedependencyplus.util.EclipseClasspathEntry;
import se.oskarnordling.intellijplugineclipsedependencyplus.util.ParseException;
import se.oskarnordling.intellijplugineclipsedependencyplus.util.XmlTools;

/**
 * @author Oskar Nordling
 */
public class AddAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        handle(e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE),
               e.getDataContext().getData(PlatformDataKeys.MODULE),
               e.getDataContext().getData(PlatformDataKeys.PROJECT));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        Module module = e.getDataContext().getData(PlatformDataKeys.MODULE);
        Project project = e.getDataContext().getData(PlatformDataKeys.PROJECT);
        if (file != null && module != null && project != null && file.getName().equalsIgnoreCase(".classpath")) {
            e.getPresentation().setVisible(!libraryExists(module, project));
        } else {
            e.getPresentation().setVisible(false);
        }
    }

    static void handle(VirtualFile file, Module module, Project project) {
        String libraryName = getLibraryName(module);
        EResult result = ApplicationManager.getApplication().runWriteAction((Computable<EResult>) () -> {
            Collection<EclipseClasspathEntry> classpathEntries;
            try {
                classpathEntries = XmlTools.getEclipseClasspathEntries(file);
            } catch (ParseException e) {
                return EResult.FAILED;
            }
            LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
            Library library = table.getLibraryByName(libraryName);
            EResult out = EResult.REFRESHED;
            if (library == null) {
                library = table.createLibrary(libraryName);
                out = EResult.CREATED;
            }
            ModifiableModel modifiableLibrary = library.getModifiableModel();
            Arrays.stream(modifiableLibrary.getUrls(OrderRootType.CLASSES))
                  .forEach(url -> modifiableLibrary.removeRoot(url, OrderRootType.CLASSES));
            Arrays.stream(modifiableLibrary.getUrls(OrderRootType.SOURCES))
                  .forEach(url -> modifiableLibrary.removeRoot(url, OrderRootType.SOURCES));
            for (EclipseClasspathEntry classpathEntry : classpathEntries) {
                modifiableLibrary.addRoot(classpathEntry.getPath(), OrderRootType.CLASSES);
                classpathEntry.getSourcePath().ifPresent(sourcePath -> modifiableLibrary.addRoot(sourcePath, OrderRootType.SOURCES));
            }
            modifiableLibrary.commit();
            if (out == EResult.CREATED) {
                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                ModifiableRootModel modifiableModuleRootManager = moduleRootManager.getModifiableModel();
                modifiableModuleRootManager.addLibraryEntry(library);
                modifiableModuleRootManager.commit();
            }
            return out;
        });
        switch (result) {
            case CREATED:
                Messages.showInfoMessage("Library " + libraryName + " created", "Dependencies Added");
                break;
            case REFRESHED:
                Messages.showInfoMessage("Library " + libraryName + " refreshed", "Dependencies Refreshed");
                break;
            case FAILED:
                Messages.showErrorDialog("Failed to create library " + libraryName, "Error");
                break;
            default:
                Messages.showErrorDialog("Failed to create library " + libraryName + ", invalid EResult: " + result.name(), "Error");
                break;
        }
    }

    static void remove(Module module, Project project) {
        String libraryName = AddAction.getLibraryName(module);
        ApplicationManager.getApplication().runWriteAction(() -> {
            LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
            Library library = table.getLibraryByName(libraryName);
            if (library != null) {
                table.removeLibrary(library);
            }
        });
        Messages.showInfoMessage("Library " + libraryName + " removed", "Dependencies Removed");
    }

    static boolean libraryExists(Module module, Project project) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
            String libraryName = getLibraryName(module);
            Library library = table.getLibraryByName(libraryName);
            if (library == null) {
                return false;
            }
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            return Arrays.stream(moduleRootManager.getOrderEntries())
                         .filter(LibraryOrderEntry.class::isInstance)
                         .map(LibraryOrderEntry.class::cast)
                         .map(LibraryOrderEntry::getLibraryName)
                         .filter(Objects::nonNull)
                         .anyMatch(libName -> libName.equals(libraryName));
        });
    }

    static String getLibraryName(Module module) {
        return module.getName() + "-ECLIPSE_DEPENEDENCIES";
    }

    private enum EResult {
        CREATED,
        REFRESHED,
        FAILED
    }
}
