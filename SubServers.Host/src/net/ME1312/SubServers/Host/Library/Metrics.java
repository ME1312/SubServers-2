package net.ME1312.SubServers.Host.Library;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Log.Logger;
import net.ME1312.SubServers.Host.ExHost;
import net.ME1312.SubServers.Host.SubAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * bStats collects some data for plugin authors.
 *
 * Check out https://bStats.org/ to learn more about bStats!
 */
public class Metrics {

    // The version of this bStats class
    public static final int B_STATS_VERSION = 1;

    // The url to which the data is sent
    private static final String URL = "https://bStats.org/submitData/other";

    // Should failed requests be logged?
    private static boolean logFailedRequests = false;

    // The logger for the failed requests
    private static final Logger logger = new Logger("bStats");

    // The name of the server software
    private final String name = "SubServers Host";

    // The uuid of the server
    private String serverUUID;

    // A list with all custom charts
    private final List<CustomChart> charts = new ArrayList<>();

    /**
     * Class constructor.
     *
     * @param host SubServers.Host
     */
    public Metrics(ExHost host) {
        boolean enabled = true;
        File configPath = new File(new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "plugins"), "bStats");
        configPath.mkdirs();
        File configFile = new File(configPath, "config.yml");
        try {
            if (!configFile.exists()) {
                FileWriter writer = new FileWriter(configFile);
                writer.write("# bStats (https://bStats.org) collects some basic information for plugin authors, like how\n");
                writer.write("# many people use their plugin and their total player count. It's recommended to keep bStats\n");
                writer.write("# enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n");
                writer.write("# performance penalty associated with having metrics enabled, and data sent to bStats is fully\n");
                writer.write("# anonymous.\n");
                writer.write("enabled: true\n");
                writer.write("serverUuid: \"" + UUID.randomUUID().toString() + "\"\n");
                writer.write("logFailedRequests: false\n");
                writer.close();
            }

            ObjectMap<String> configuration = new YAMLConfig(configFile).get();

            // Load configuration
            enabled = configuration.getBoolean("enabled", true);
            serverUUID = configuration.getString("serverUuid");
            logFailedRequests = configuration.getBoolean("logFailedRequests", false);

            // Load charts
            charts.add(new SingleLineChart("servers", () -> 1));
            charts.add(new SingleLineChart("hosted_servers", () -> host.servers.size()));
            charts.add(new SingleLineChart("plugins", () -> host.engine.getPluginManager().getPlugins().size()));
            charts.add(new SimplePie("engineVersion", () -> host.engine.getEngineInfo().getVersion().toString()));
            charts.add(new SimplePie("pluginVersion", () -> host.info.getVersion().toString()));
            charts.add(new DrilldownPie("os", () -> {
                String id = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
                String name = System.getProperty("os.name");
                String version = System.getProperty("os.version");
                Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
                Map<String, Integer> imap = new HashMap<String, Integer>();

                if (id.contains("mac") || id.contains("darwin")) {
                    imap.put("Mac OS " + version, 1);
                    map.put("Mac OS", imap);
                } else if (id.contains("win")) {
                    imap.put(name, 1);
                    if (id.contains("server")) {
                        map.put("Windows Server", imap);
                    } else {
                        map.put("Windows", imap);
                    }
                } else if (id.contains("bsd")) {
                    imap.put(name + ' ' + version, 1);
                    map.put("BSD", imap);
                } else if (id.contains("nux")) {
                    imap.put(version, 1);
                    map.put("Linux", imap);
                } else {
                    imap.put(name + ' ' + version, 1);
                    map.put(name, imap);
                }

                return map;
            }));
            charts.add(new SimplePie("coreCount", () -> Integer.toString(Runtime.getRuntime().availableProcessors())));
            charts.add(new DrilldownPie("javaVersion", () -> {
                String version = System.getProperty("java.version");
                Matcher regex = Pattern.compile("(?:1\\.)?(\\d+).*").matcher(version);
                Map<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();
                Map<String, Integer> imap = new HashMap<String, Integer>();
                if (regex.find()) {
                    imap.put(regex.group(), 1);
                    map.put("Java " + regex.group(1), imap);
                } else {
                    imap.put(version, 1);
                    map.put("Java X", imap);
                }
                return map;

            }));
            charts.add(new SimplePie("osArch", () -> System.getProperty("os.arch")));

        } catch (Exception e) {
            logger.error.println(e);
        }

        // We are allowed to send data
        if (enabled) {
            // Start submitting the data
            startSubmitting();
        }
    }

    /**
     * Starts the Scheduler which submits our data every 30 minutes.
     */
    private void startSubmitting() {
        final Timer timer = new Timer(SubAPI.getInstance().getAppInfo().getName() + "::Metrics_Uploader", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                submitData();
            }
        }, 1000*60*5, 1000*60*30);
        // Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough time to start
        // WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
        // WARNING: Just don't do it!
    }

    /**
     * Gets the plugin specific data.
     *
     * @return The plugin specific data.
     */
    private JSONObject getPluginData() {
        JSONObject data = new JSONObject();

        data.put("pluginName", name); // Append the name of the server software
        JSONArray customCharts = new JSONArray();
        for (CustomChart customChart : charts) {
            // Add the data of the custom charts
            JSONObject chart = customChart.getRequestJsonObject();
            if (chart == null) { // If the chart is null, we skip it
                continue;
            }
            customCharts.put(chart);
        }
        data.put("customCharts", customCharts);

        return data;
    }

    /**
     * Gets the server specific data.
     *
     * @return The server specific data.
     */
    private JSONObject getServerData() {
        JSONObject data = new JSONObject();
        data.put("serverUUID", serverUUID);
        return data;
    }

    /**
     * Collects the data and sends it afterwards.
     */
    private void submitData() {
        final JSONObject data = getServerData();

        JSONArray pluginData = new JSONArray();
        pluginData.put(getPluginData());
        data.put("plugins", pluginData);

        try {
            // We are still in the Thread of the timer, so nothing get blocked :)
            sendData(data);
        } catch (Exception e) {
            // Something went wrong! :(
            if (logFailedRequests) {
                logger.warn.println("Could not submit stats of " + name, e);
            }
        }
    }

    /**
     * Sends the data to the bStats server.
     *
     * @param data The data to send.
     * @throws Exception If the request failed.
     */
    private static void sendData(JSONObject data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null!");
        }

        HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();

        // Compress the data to save bandwidth
        byte[] compressedData = compress(data.toString());

        // Add headers
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
        connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

        // Send data
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);
        outputStream.flush();
        outputStream.close();

        InputStream inputStream = connection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            builder.append(line);
        }
        bufferedReader.close();
    }

    /**
     * Gzips the given String.
     *
     * @param str The string to gzip.
     * @return The gzipped String.
     * @throws IOException If the compression failed.
     */
    private static byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return outputStream.toByteArray();
    }

    /**
     * Represents a custom chart.
     */
    public static abstract class CustomChart {

        // The id of the chart
        final String chartId;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         */
        CustomChart(String chartId) {
            if (chartId == null || chartId.isEmpty()) {
                throw new IllegalArgumentException("ChartId cannot be null or empty!");
            }
            this.chartId = chartId;
        }

        private JSONObject getRequestJsonObject() {
            JSONObject chart = new JSONObject();
            chart.put("chartId", chartId);
            try {
                JSONObject data = getChartData();
                if (data == null) {
                    // If the data is null we don't send the chart.
                    return null;
                }
                chart.put("data", data);
            } catch (Throwable t) {
                if (logFailedRequests) {
                    logger.warn.println("Failed to get data for custom chart with id " + chartId, t);
                }
                return null;
            }
            return chart;
        }

        protected abstract JSONObject getChartData() throws Exception;

    }

    /**
     * Represents a custom simple pie.
     */
    public static class SimplePie extends CustomChart {

        private final Callable<String> callable;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         * @param callable The callable which is used to request the chart data.
         */
        public SimplePie(String chartId, Callable<String> callable) {
            super(chartId);
            this.callable = callable;
        }

        @Override
        protected JSONObject getChartData() throws Exception {
            JSONObject data = new JSONObject();
            String value = callable.call();
            if (value == null || value.isEmpty()) {
                // Null = skip the chart
                return null;
            }
            data.put("value", value);
            return data;
        }
    }

    /**
     * Represents a custom drilldown pie.
     */
    public static class DrilldownPie extends CustomChart {

        private final Callable<Map<String, Map<String, Integer>>> callable;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         * @param callable The callable which is used to request the chart data.
         */
        public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
            super(chartId);
            this.callable = callable;
        }

        @Override
        public JSONObject getChartData() throws Exception {
            JSONObject data = new JSONObject();
            JSONObject values = new JSONObject();
            Map<String, Map<String, Integer>> map = callable.call();
            if (map == null || map.isEmpty()) {
                // Null = skip the chart
                return null;
            }
            boolean reallyAllSkipped = true;
            for (Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
                JSONObject value = new JSONObject();
                boolean allSkipped = true;
                for (Map.Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
                    value.put(valueEntry.getKey(), valueEntry.getValue());
                    allSkipped = false;
                }
                if (!allSkipped) {
                    reallyAllSkipped = false;
                    values.put(entryValues.getKey(), value);
                }
            }
            if (reallyAllSkipped) {
                // Null = skip the chart
                return null;
            }
            data.put("values", values);
            return data;
        }
    }

    /**
     * Represents a custom single line chart.
     */
    public static class SingleLineChart extends CustomChart {

        private final Callable<Integer> callable;

        /**
         * Class constructor.
         *
         * @param chartId The id of the chart.
         * @param callable The callable which is used to request the chart data.
         */
        public SingleLineChart(String chartId, Callable<Integer> callable) {
            super(chartId);
            this.callable = callable;
        }

        @Override
        protected JSONObject getChartData() throws Exception {
            JSONObject data = new JSONObject();
            int value = callable.call();
            if (value == 0) {
                // Null = skip the chart
                return null;
            }
            data.put("value", value);
            return data;
        }

    }
}