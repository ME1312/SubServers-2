package net.ME1312.SubServers.Proxy.Host.Internal;

import net.ME1312.SubServers.Proxy.Library.Container;
import net.md_5.bungee.api.ProxyServer;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalSubLogger extends Thread {
    InputStream is;
    String name;
    Container<Boolean> log;
    PrintWriter writer = null;

    InternalSubLogger(InputStream is, String name, Container<Boolean> log, File file) {
        this.is = is;
        this.name = name;
        this.log = log;
        if (file != null)
            try {
                this.writer = new PrintWriter(file, "UTF-8");
                this.writer.println("---------- LOG START: " + name + " ----------");
                this.writer.flush();
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith(">")) {
                    if (log.get()) {
                        String msg = line;
                        // REGEX Formatting
                        String type = "INFO";
                        Matcher matcher = Pattern.compile("^((?:\\s*\\[?[0-9]{2}:[0-9]{2}:[0-9]{2}]?)?\\s*(?:\\[|\\[.*\\/)?(INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]:?\\s*)").matcher(msg);
                        while (matcher.find()) {
                            type = matcher.group(2);
                        }

                        msg = msg.replaceAll("^((?:\\s*\\[?[0-9]{2}:[0-9]{2}:[0-9]{2}]?)?\\s*(?:\\[|\\[.*\\/)?(INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]:?\\s*)", "");

                        switch (type) {
                            case "INFO":
                                ProxyServer.getInstance().getLogger().info(name + " > " + msg);
                                break;
                            case "WARNING":
                            case "WARN":
                                ProxyServer.getInstance().getLogger().warning(name + " > " + msg);
                                break;
                            case "SEVERE":
                            case "ERROR":
                            case "ERR":
                                ProxyServer.getInstance().getLogger().severe(name + " > " + msg);
                                break;
                        }
                    }
                    if (writer != null) {
                        writer.println(line);
                        writer.flush();
                    }
                }
            }
        } catch (IOException ioe) {} finally {
            if (writer != null) {
                writer.println("---------- END LOG ----------");
                writer.close();
            }
        }
    }
}
