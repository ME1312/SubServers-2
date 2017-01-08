package net.ME1312.SubServers.Client.Bukkit.Library;

import java.io.*;

public final class Util {
    private Util(){}
    public interface ExceptionRunnable {
        void run() throws Throwable;
    }

    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static void copyFromJar(ClassLoader loader, String resource, String destination) {
        InputStream resStreamIn = loader.getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isSpigot() {
        final Container<Boolean> spigot = new Container<Boolean>(false);
        return !isException(() -> {
            if (Class.forName("org.spigotmc.SpigotConfig") != null) spigot.set(true);
        }) && spigot.get();
    }

    public static boolean isException(ExceptionRunnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (Throwable e) {
            return true;
        }
    }

    public static String unescapeJavaString(String str) {

        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == str.length() - 1) ? '\\' : str
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                            && str.charAt(i + 1) <= '7') {
                        code += str.charAt(i + 1);
                        i++;
                        if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                                && str.charAt(i + 1) <= '7') {
                            code += str.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= str.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + str.charAt(i + 2) + str.charAt(i + 3)
                                        + str.charAt(i + 4) + str.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
