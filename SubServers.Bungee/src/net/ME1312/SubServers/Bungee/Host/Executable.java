package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.SubServers.Bungee.Library.Compatibility.JNA;
import net.ME1312.SubServers.Bungee.Library.Util;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Executable Handler Class
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
            if (gitbash != null && (exec.toLowerCase().startsWith("bash ") || exec.toLowerCase().startsWith("sh ")))
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
     * @return Process ID (null if unknown)
     */
    public static Long pid(Process process) {
        if (process.isAlive()) {
            try {
                return (long) Process.class.getDeclaredMethod("pid").invoke(process);
            } catch (Throwable ex) {
                try {
                    if (process.getClass().getName().equals("java.lang.Win32Process") || process.getClass().getName().equals("java.lang.ProcessImpl")) {
                        long handle = Util.reflect(process.getClass().getDeclaredField("handle"), process);

                        ClassLoader jna = JNA.get();
                        Class<?> pc = jna.loadClass("com.sun.jna.Pointer"),
                                ntc = jna.loadClass("com.sun.jna.platform.win32.WinNT$HANDLE"),
                                k32c = jna.loadClass("com.sun.jna.platform.win32.Kernel32");
                        Object k32 = k32c.getField("INSTANCE").get(null),
                                nt = ntc.getConstructor().newInstance();
                        ntc.getMethod("setPointer", pc).invoke(nt, pc.getMethod("createConstant", long.class).invoke(null, handle));
                        return ((Number) k32c.getMethod("GetProcessId", ntc).invoke(k32, nt)).longValue();
                    } else if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                        Object response = Util.reflect(process.getClass().getDeclaredField("pid"), process);

                        if (response instanceof Number)
                            return ((Number) response).longValue();
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
                    Process terminator = Runtime.getRuntime().exec(new String[]{"taskkill.exe", "/T", "/F", "/PID", pid.toString()});
                    terminator.waitFor();
                } catch (Throwable e) {}
            }
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
