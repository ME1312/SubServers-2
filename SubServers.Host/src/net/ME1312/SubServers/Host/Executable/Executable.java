package net.ME1312.SubServers.Host.Executable;

import net.ME1312.Galaxi.Library.Util;

import java.io.File;
import java.io.Serializable;

/**
 * Executable Variable Class
 */
@SuppressWarnings("serial")
public class Executable implements Serializable {
    private boolean isFile;
    private File File;
    private String Str;
    /**
     * New Executable
     *
     * @param exe Executable String or File Path
     */
    public Executable(String exe) {
        if (Util.isNull(exe)) throw new NullPointerException();
        if (new File(exe).exists()) {
            isFile = true;
            File = new File(exe);
            Str = exe;
        } else {
            isFile = false;
            File = null;
            Str = exe;
        }
    }

    /**
     * New Executable
     *
     * @param path File Path
     */
    public Executable(File path) {
        if (Util.isNull(path)) throw new NullPointerException();
        isFile = true;
        File = path;
        Str = path.toString();
    }

    @Override
    public String toString() {
        String String;
        if (isFile) {
            String = File.toString();
        } else {
            String = Str;
        }
        return String;
    }

    /**
     * Check if the Executable String is a file
     *
     * @return File Status
     */
    public boolean isFile() {
        return isFile;
    }

    /**
     * Get Executable File
     *
     * @return File or Null if Executable isn't a file
     */
    public File toFile() {
        return File;
    }

}
