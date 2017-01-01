package net.ME1312.SubServers.Bungee.Host.Internal;

import net.ME1312.SubServers.Bungee.Library.Container;
import net.md_5.bungee.api.ProxyServer;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalSubLogger {
    private Process process;
    private String name;
    private Container<Boolean> log;
    private PrintWriter writer = null;
    private boolean started = false;

    InternalSubLogger(Process process, String name, Container<Boolean> log, File file) {
        this.process = process;
        this.name = name;
        this.log = log;
        if (file != null)
            try {
                this.writer = new PrintWriter(file, "UTF-8");
            } catch (UnsupportedEncodingException | FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    public void start() {
        started = true;
        if (writer != null) {
            this.writer.println("---------- LOG START: " + name + " ----------");
            this.writer.flush();
        }
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(">")) {
                        if (log.get()) {
                            String msg = line;
                            // REGEX Formatting
                            String type = "INFO";
                            Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)").matcher(msg);
                            while (matcher.find()) {
                                type = matcher.group(3).toUpperCase();
                            }

                            msg = msg.replaceAll("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)", "");

                            switch (type) {
                                case "INFO":
                                case "MESSAGE":
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
            } catch (IOException e) {} finally {
                stop();
            }
        }).start();
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(">")) {
                        if (log.get()) {
                            String msg = line;
                            // REGEX Formatting
                            String type = "INFO";
                            Matcher matcher = Pattern.compile("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)").matcher(msg);
                            while (matcher.find()) {
                                type = matcher.group(3).toUpperCase();
                            }

                            msg = msg.replaceAll("^((?:\\s*\\[?([0-9]{2}:[0-9]{2}:[0-9]{2})]?)?[\\s\\/\\\\\\|]*(?:\\[|\\[.*\\/)?(MESSAGE|INFO|WARN|WARNING|ERROR|ERR|SEVERE)\\]?:?\\s*)", "");

                            switch (type) {
                                case "INFO":
                                case "MESSAGE":
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
            } catch (IOException e) {} finally {
                stop();
            }
        }).start();
    }

    private void stop() {
        if (started) {
            started = false;
            if (writer != null) {
                writer.println("---------- END LOG ----------");
                writer.close();
            }
        }
    }
}
