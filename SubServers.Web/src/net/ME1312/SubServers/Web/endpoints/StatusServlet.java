package net.ME1312.SubServers.Web.endpoints;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StatusServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);

		Map<String, Object> map = new HashMap<>();
		map.put("status", "ok");

		response.getWriter().println(new Gson().toJson(map));
	}
}
