package net.ME1312.SubServers.Bungee.Host;

import java.io.File;
import java.io.Serializable;

/**
 * Executable Variable Class
 */
@SuppressWarnings("serial")
public class Executable implements Serializable {
    public boolean isFile;
    private File File;
    private String Str;
    /**
     * New Executable
     *
     * @param exe Executable String or File Path
     */
    public Executable(String exe) {
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
     * @param Path File Path
     */
    public Executable(File Path) {
        isFile = true;
        File = Path;
        Str = Path.toString();
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
     * Get Executable File
     *
     * @return File or Null if Executable isn't a file
     */
    public File toFile() {
        return File;
    }

}
