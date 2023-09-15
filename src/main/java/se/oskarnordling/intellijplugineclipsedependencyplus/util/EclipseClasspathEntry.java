package se.oskarnordling.intellijplugineclipsedependencyplus.util;

import java.util.Optional;

/**
 * @author Oskar Nordling
 */
public class EclipseClasspathEntry {
    private String path;
    private String sourcePath;

    public EclipseClasspathEntry(String path, String sourcePath) {
        this.path = path;
        this.sourcePath = sourcePath;
    }

    /**
     * Gets the {@code path}
     *
     * @return the {@code path}
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the {@code sourcePath}
     *
     * @return the {@code sourcePath}
     */
    public Optional<String> getSourcePath() {
        return Optional.ofNullable(sourcePath);
    }
}
