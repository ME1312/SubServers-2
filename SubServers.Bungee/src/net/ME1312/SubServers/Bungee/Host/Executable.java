package net.ME1312.SubServers.Bungee.Host;

import java.io.File;

/**
 * Executable String Handler Class
 */
public class Executable {
    private Executable() {}

    /**
     * Format a command to be executed
     *
     * @param gitbash Git Bash location (optional)
     * @param exec Executable String
     * @return
     */
    public static String[] parse(String gitbash, String exec) {
        String[] cmd;
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            if (gitbash != null && (exec.startsWith("bash ") || exec.startsWith("sh ")))
                exec = "\"" + gitbash + ((gitbash.endsWith(File.separator))?"":File.separator) + "bin" + File.separatorChar + "sh.exe\" -lic \"" +
                        exec.replace("\\", "/\\").replace("\"", "\\\"").replace("^", "^^").replace("%", "^%").replace("&", "^&").replace("<", "^<").replace(">", "^>").replace("|", "^|") + "\"";
            cmd = new String[]{"cmd.exe", "/q", "/c", '"'+exec+'"'};
        } else {
            cmd = new String[]{"sh", "-lic", exec};
        }
        return cmd;
    }
}
