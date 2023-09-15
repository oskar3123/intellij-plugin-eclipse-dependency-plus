package se.oskarnordling.intellijplugineclipsedependencyplus.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Oskar Nordling
 */
public class XmlTools {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final Pattern URL_PATTERN = Pattern.compile("[a-zA-Z]+:[/\\\\].+");

    public static Collection<EclipseClasspathEntry> getEclipseClasspathEntries(VirtualFile file) throws ParseException {
        Document document = parseFile(file);
        NodeList nodeList = document.getDocumentElement().getChildNodes();

        Collection<EclipseClasspathEntry> out = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            if (node.getNodeName().equalsIgnoreCase("classpathentry") && attributes != null) {
                String kind = getNamedItem(attributes, "kind");
                String path = getNamedItem(attributes, "path");
                String sourcePath = getNamedItem(attributes, "sourcepath");
                if (kind != null && path != null && kind.equalsIgnoreCase("lib")) {
                    String formattedPath = formatPath(path, file.getParent().getPath());
                    String formattedSourcePath = sourcePath != null ? formatPath(sourcePath, file.getParent().getPath()) : null;
                    out.add(new EclipseClasspathEntry(formattedPath, formattedSourcePath));
                }
            }
        }
        return out;
    }

    private static Document parseFile(VirtualFile file) throws ParseException {
        try {
            return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse(file.getInputStream());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new ParseException(e);
        }
    }

    private static String getNamedItem(NamedNodeMap attributes, String name) {
        return Optional
                .ofNullable(attributes.getNamedItem(name))
                .map(Node::getNodeValue)
                .orElse(null);
    }

    private static String formatPath(String path, String directoryPath) {
        boolean isUrl = URL_PATTERN.matcher(path).matches();
        if (!path.startsWith("/") && !isUrl) {
            path = directoryPath + "/" + path;
        }
        if (isUrl) {
            return path;
        } else if (path.endsWith(".jar")) {
            return "jar://" + path + "!/";
        } else {
            return "file://" + path;
        }
    }
}
