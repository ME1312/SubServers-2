package net.ME1312.SubServers.Bungee.Host;

import net.ME1312.Galaxi.Library.Platform;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.SubServers.Bungee.Library.Compatibility.JNA;

import java.io.File;
import java.io.IOException;

/**
 * Executable Handler Class
 */
public class Executable {
    private Executable() {}
    private static final boolean USE_SESSION_TRACKING;

    /**
     * Format a command to be executed
     *
     * @param gitbash Git Bash location (optional)
     * @param exec Executable String
     * @return Formatted Executable
     */
    public static String[] parse(String gitbash, String exec) {
        if (exec.startsWith("java "))
            exec = '\"' + System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" + '\"' + exec.substring(4);

        String[] cmd;
        if (Platform.getSystem() == Platform.WINDOWS) {
            if (gitbash != null && (exec.toLowerCase().startsWith("bash ") || exec.toLowerCase().startsWith("sh ")))
                exec = '"' + gitbash + ((gitbash.endsWith(File.separator))?"":File.separator) + "bin" + File.separatorChar + "sh.exe\" -lc \"" +
                        exec.replace("\\", "/\\").replace("\"", "\\\"").replace("^", "^^").replace("%", "^%").replace("&", "^&").replace("<", "^<").replace(">", "^>").replace("|", "^|") + '"';
            cmd = new String[]{"cmd.exe", "/q", "/c", '"'+exec+'"'};
        } else if (USE_SESSION_TRACKING) {
            cmd = new String[]{"setsid", "-w", "sh", "-lc", exec};
        } else {
            cmd = new String[]{"sh", "-lc", exec};
        }
        return cmd;
    }

    static {
        USE_SESSION_TRACKING = Platform.getSystem() != Platform.WINDOWS && Util.getDespiteException(() -> {
            Process test = Runtime.getRuntime().exec(new String[]{"setsid", "-w", "bash", "-c", "exit 0"});
            test.waitFor(); // The purpose of this block is to test for the 'setsid' command
            return test.exitValue() == 0;
        }, false);
    }

    /**
     * Get the PID of a currently running process
     *
     * @param process Process
     * @return Process ID (null if unknown)
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    public static Long pid(Process process) {
        if (process.isAlive()) {
            try { // Java 9 Standard
                return (long) Process.class.getMethod("pid").invoke(process);
            } catch (Throwable e) {
                try { // Java 8 Not-so-standard
                    Object response = Util.reflect(process.getClass().getDeclaredField("pid"), process);

                    if (response instanceof Number) {
                        return ((Number) response).longValue();
                    } else throw e;
                } catch (Throwable e2) {
                    if (Platform.getSystem() == Platform.WINDOWS) try {
                        long handle = Util.reflect(process.getClass().getDeclaredField("handle"), process);

                        ClassLoader jna = JNA.get();
                        Class<?> pc = jna.loadClass("com.sun.jna.Pointer"),
                                ntc = jna.loadClass("com.sun.jna.platform.win32.WinNT$HANDLE"),
                               k32c = jna.loadClass("com.sun.jna.platform.win32.Kernel32");
                        Object k32 = k32c.getField("INSTANCE").get(null),
                                nt = ntc.getConstructor().newInstance();
                        ntc.getMethod("setPointer", pc).invoke(nt, pc.getMethod("createConstant", long.class).invoke(null, handle));
                        return ((Number) k32c.getMethod("GetProcessId", ntc).invoke(k32, nt)).longValue();
                    } catch (Throwable e3) {
                        // No way to find pid, I suppose.
                    }
                }
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
            Long pid = pid(process);
            if (pid != null) try {
                if (Platform.getSystem() == Platform.WINDOWS) {
                    Runtime.getRuntime().exec(new String[]{"taskkill.exe", "/T", "/F", "/PID", pid.toString()}).waitFor();
                } else if (USE_SESSION_TRACKING) {
                    Runtime.getRuntime().exec(new String[]{"bash", "-c", "kill -9 $(ps -o pid= --sid $(ps -o sid= --pid " + pid + "))"}).waitFor();
                }
            } catch (IOException | InterruptedException e) {}

            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
