package rt.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
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

import org.json.JSONArray;
import org.json.JSONObject;

import oracle.jdbc.OracleTypes;

public class Call4Result extends HttpServlet {

	private static final long serialVersionUID = 11L;
	private static final String ERROR_PREX = "ERROR_SERVER:";

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/json;charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		java.sql.Connection conn = null;
		java.sql.Statement stmt = null;
		ResultSet resultSet = null;

		try {
			Context ctx = new InitialContext();

			DataSource ds = (DataSource) ctx.lookup("jndi-lj");
			// Create a connection object
			conn = ds.getConnection();
			//conn.setAutoCommit(true);
			
			String keywords = new String(request.getParameter("keywords").getBytes("ISO-8859-1"), "UTF-8");
			int rowFrom = Integer.parseInt(new String(request.getParameter("from").getBytes("ISO-8859-1"), "UTF-8")) ;
			int rowTo = Integer.parseInt(new String(request.getParameter("to").getBytes("ISO-8859-1"), "UTF-8")) ;
			JSONArray jsonArray = new JSONArray();		
			
			CallableStatement cs = conn.prepareCall("Call lj.lj_p_search(?,?,?,?,?)");
			cs.setString(1, keywords);
			cs.setInt(2, rowFrom);
			cs.setInt(3, rowTo);
			cs.registerOutParameter(4,OracleTypes.CURSOR);
			cs.registerOutParameter(5,OracleTypes.VARCHAR);
			cs.execute();
			
			resultSet = (ResultSet) cs.getObject(4);	
			String msg = cs.getString(5);
			
			if (msg!=null){
				out.print(ERROR_PREX + msg);
			}else{
				ResultSetMetaData metaData = resultSet.getMetaData();
				int columnCount = metaData.getColumnCount();
				while (resultSet!=null && resultSet.next()) {
					JSONObject jObject = new JSONObject();
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metaData.getColumnLabel(i);
						String columnValue = resultSet.getString(columnName);
						jObject.put(columnName, columnValue);
					}
					jsonArray.put(jObject);
				}
				out.print(jsonArray.toString());
			}			

		} catch (Exception e) {
			e.printStackTrace();
			out.print(ERROR_PREX + e.getMessage());
		} finally {
			// ============ Close JDBC objects, including the connection =======
			try {
				cleanup(resultSet, stmt, conn);
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