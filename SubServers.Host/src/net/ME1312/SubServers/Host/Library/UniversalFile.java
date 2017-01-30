package net.ME1312.SubServers.Host.Library;

import java.io.File;

/**
 * Universal File Class
 */
public class UniversalFile extends File {

    /**
     * Creates a File Link. Path names are separated by ':'
     *
     * @param pathname Path name
     */
    public UniversalFile(String pathname) {
        super(pathname.replace(".:", System.getProperty("user.dir") + ":").replace(':', File.separatorChar));
    }

    /**
     * Creates a File Link. Path names are separated by the divider
     *
     * @param pathname Path name
     * @param divider Divider to use
     */
    public UniversalFile(String pathname, char divider) {
        super(pathname.replace("." + divider, System.getProperty("user.dir") + divider).replace(divider, File.separatorChar));
    }

    /**
     * Creates a File Link.
     *
     * @see File
     * @param file File
     */
    public UniversalFile(File file) {
        super(file.getPath());
    }

    /**
     * Creates a File. Path names are separated by the ':'
     *
     * @see File
     * @param parent Parent File
     * @param child Path name
     */
    public UniversalFile(File parent, String child) {
        super(parent, child.replace(':', File.separatorChar));
    }

    /**
     * Creates a File. Path names are separated by the divider
     *
     * @see File
     * @param parent Parent File
     * @param child Path name
     * @param divider Divider to use
     */
    public UniversalFile(File parent, String child, char divider) {
        super(parent, child.replace(divider, File.separatorChar));
    }

    /**
     * Gets the Universal File Path (separated by ':')
     *
     * @return
     */
    public String getUniversalPath() {
        return getPath().replace(File.separatorChar, ':');
    }
}
