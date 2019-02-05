package com.jps.finserv.markets.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;

public class DBConnectionPgSql {

	private final Logger logger = (Logger) LoggerFactory.getLogger(DBConnectionPgSql.class);
	
	private final String url = "jdbc:postgresql://localhost/postgres";
	private final String username = "postgres";
	private final String password = "$ys8dmin";
	
	public void testConn(){
		
		try {
			Connection conn = DriverManager.getConnection(url, username, password);
			//String url = "jdbc:postgresql://localhost/test";
			//Connection conn = DriverManager.getConnection(url);
			
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM postgres");// WHERE columnfoo = 500");
			while (rs.next())
			{
			    System.out.print("Column 1 returned ");
			    System.out.println(rs.getString(1));
			}
			rs.close();
			st.close();
			/*
			 * TODO: Enable after psql server is accepting SSL conn in our setup. 
			 
			Properties props = new Properties();
			props.setProperty("user", username);
			props.setProperty("password", password);
			props.setProperty("ssl","true");
			Connection conn = DriverManager.getConnection(url, props);
			 */
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}