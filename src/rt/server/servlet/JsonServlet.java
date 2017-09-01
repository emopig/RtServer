package rt.server.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.json.JSONObject;


public class JsonServlet extends HttpServlet {

	private static final long serialVersionUID = 11L;
	private static final String ERROR_PREX = "ERROR_SERVER:";

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		java.sql.Connection conn = null;
		java.sql.Statement stmt = null;
		ResultSet rs = null;

		try {
			Context ctx = new InitialContext();
			Context envCtx = (Context) ctx.lookup("java:/comp/env");
			//DataSource ds = (DataSource) ctx.lookup("jndi-lj");
			DataSource ds = (DataSource) envCtx.lookup("jndi-lj");
			
			// Create a connection object
			conn = ds.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();		
			

			//String url = null;//this.getServletContext().getRealPath("/");
			String url = "http://df049m:7001" + this.getServletContext().getContextPath() + "/json/" + new String(request.getParameter("block").getBytes("ISO-8859-1"), "UTF-8") + ".json";
			JSONObject jsonObject = getJson(url);
			
			String sql = jsonObject.getString("sql");
			jsonObject.remove("sql");
			// //
			ResultSet resultSet = stmt.executeQuery(sql);

			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			while (resultSet.next()) {
				JSONObject jObject = new JSONObject();
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metaData.getColumnLabel(i);
					String columnValue = resultSet.getString(columnName);
					jObject.put(columnName, columnValue);
				}
				jsonObject.append("data", jObject);
			}
			out.print(jsonObject.toString());

		} catch (Exception e) {
			e.printStackTrace();
			out.print(ERROR_PREX + e.getMessage());
		} finally {
			// ============ Close JDBC objects, including the connection =======
			try {
				cleanup(rs, stmt, conn);
				return;
			} catch (SQLException e) {
				e.printStackTrace();
				out.print(ERROR_PREX + e.getMessage());
			}
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}


	public JSONObject getJson(String _url) throws Exception{
		JSONObject jsonObject = null;
		BufferedReader reader = null;
		String laststr = "";
		try{
			URL url = new URL(_url);
			URLConnection uc = url.openConnection();
			InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream(),"UTF-8");
			reader = new BufferedReader(inputStreamReader);
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				laststr += tempString;
			}
			reader.close();
			jsonObject = new JSONObject(laststr);
		}catch(Exception e){
			throw e;
		}finally {
			if (reader != null) {
				reader.close();
			}
		}			
		return jsonObject;
	}

	private void cleanup(ResultSet resultSet, Statement stmt, Connection conn) throws SQLException {
		try {
			if (resultSet != null) {
				resultSet.close();
				resultSet = null;
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					throw e;
				}
			}
		}
	}
}
