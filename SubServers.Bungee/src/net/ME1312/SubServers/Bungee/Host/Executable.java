package net.ME1312.SubServers.Bungee.Host;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import java.io.File;
import java.lang.reflect.Field;

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
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            if (gitbash != null && (exec.startsWith("bash ") || exec.startsWith("sh ")))
                exec = '"' + gitbash + ((gitbash.endsWith(File.separator))?"":File.separator) + "bin" + File.separatorChar + "sh.exe\" -lc \"" +
                        exec.replace("\\", "/\\").replace("\"", "\\\"").replace("^", "^^").replace("%", "^%").replace("&", "^&").replace("<", "^<").replace(">", "^>").replace("|", "^|") + '"';
            cmd = new String[]{"cmd.exe", "/q", "/c", '"'+exec+'"'};
        } else {
            cmd = new String[]{"sh", "-lc", exec};
        }
        return cmd;
    }

    /**
     * Get the PID of a currently running process
     *
     * @param process Process
     * @return Process ID
     */
    public static Long pid(Process process) {
        if (process.isAlive()) {
            try {
                return (long) Process.class.getDeclaredMethod("pid").invoke(process);
            } catch (Throwable ex) {
                try {
                    if (process.getClass().getName().equals("java.lang.Win32Process") || process.getClass().getName().equals("java.lang.ProcessImpl")) {
                        Field f = process.getClass().getDeclaredField("handle");
                        f.setAccessible(true);
                        long handle = f.getLong(process);
                        f.setAccessible(false);

                        Kernel32 k32 = Kernel32.INSTANCE;
                        WinNT.HANDLE nt = new WinNT.HANDLE();
                        nt.setPointer(Pointer.createConstant(handle));
                        return (long) k32.GetProcessId(nt);
                    } else if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                        Field f = process.getClass().getDeclaredField("pid");
                        f.setAccessible(true);
                        Object response = f.get(process);
                        f.setAccessible(false);

                        if (response instanceof Number) return ((Number) response).longValue();
                    }
                } catch (Throwable e) {}
            }
        }
        return null;
    }

    /**
     * Terminate a currently running process
     *
     * @param process Process
     */
    public static void terminate(Process process) {
        if (process.isAlive()) {
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                Long pid = pid(process);
                if (pid != null) try {
                    Process terminator = Runtime.getRuntime().exec(new String[]{"taskkill", "/T", "/F", "/PID", pid.toString()});
                    terminator.waitFor();
                } catch (Throwable e) {}
            }
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
