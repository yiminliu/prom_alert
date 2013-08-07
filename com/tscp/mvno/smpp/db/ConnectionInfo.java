package com.tscp.mvno.smpp.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionInfo {
	
	// [start] member fields
    private Connection conn;
//    private static String dbHost;
//    private static String dbPort;
//    private static String dbUserName;
//    private static String dbPwd;
//    private static String dbSid;
    private int maxConnections = 50;    
    
    private String dbType;
    private String dbDriver;
    private String dbServer;
    private String dbLogin;
    private String dbPassword;
    
    private String inputProperties = "dbconfig.properties";
    
    // [end] member fields
    
    // [start] get methods
    
    public String getDbDriver() {
		return dbDriver;
	}

	public String getDbLogin() {
		return dbLogin;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public String getDbServer() {
		return dbServer;
	}

	public String getDbType() {
		return dbType;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	// [end] get methods
	
	// [start] set methods
	
	public void setDbDriver(String dbDriver) {
		this.dbDriver = dbDriver;
	}

	public void setDbLogin(String dbLogin) {
		this.dbLogin = dbLogin;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public void setDbServer(String dbServer) {
		this.dbServer = dbServer;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
	
	// [end] set methods
    
    // [start] constructors

	public ConnectionInfo() {
    	init();
    }
    
    // [end] constructors
    
    // [start] public methods
    
    public synchronized Connection getConnection() {
		conn = null;
		try {
			conn = DriverManager.getConnection(getDbServer(),getDbLogin(),getDbPassword());
		} catch( Exception e ) { 
			System.out.println("Error obtaining database connection!! "+e.getMessage());
		}
		return conn;
	}
    
    public synchronized void releaseConnection() {
    	if( conn != null ) {
    		try {
    		conn.close();
    		} catch( SQLException sql_ex ) {
    			System.out.println("SQL Exception thrown when closing connection..."+sql_ex.getMessage());
    		} catch( Exception ex ) {
    			System.out.println("General Exception thrown when closing connection..."+ex.getMessage());
    			
    		} finally {
    			conn = null;
    		}
    	}
    }
    
    // [end] public methods
    
    // [start] private methods
    
    private void init() {
    	Properties props = new Properties();
    	ClassLoader cl = ConnectionInfo.class.getClassLoader();
    	InputStream in = cl.getResourceAsStream(inputProperties);
    		
    	try {
    		if(in != null)
    		props.load(in);
//    		dbHost 			
//    		dbPort
//    		dbUserName
//    		dbPwd
//    		dbSid
    		    		
    		dbType		= props.getProperty("dbType", "Oracle");
    		dbDriver	= props.getProperty("dbDriver","oracle.jdbc.driver.OracleDriver");
    		//dbServer	= props.getProperty("dbServer","jdbc:oracle:thin:@USCAELMUX18:1521:K11MVNOT");
    		//dbServer	= props.getProperty("dbServer","jdbc:oracle:thin:@K11MVNO:1521:K11MVNOT");
    		dbServer	= props.getProperty("dbServer","jdbc:oracle:thin:@uscael200:1521:K11MVNOT");
    		
    		
    		dbLogin		= props.getProperty("dbLogin", "REPORT");
    		dbPassword	= props.getProperty("dbPassword", "REPORTMVNO");
    	
    		Class.forName(dbDriver).newInstance();
    	} catch ( Exception e ) {
    		e.printStackTrace();
    		System.out.println("Error loading DB Properties!!");
    	}
    }
    
    // [end] private methods
    
    // [start] main method
    
    public static void main(String[] args) { 
    	System.out.println("Testing SMPP Project ConnectionInfo class....");
    	ConnectionInfo ci = new ConnectionInfo();
    	System.out.println("ConnectionInfo initialized...");
    	System.out.println("**** ConnectionInfo.DBType            :: "+ci.getDbType());
    	System.out.println("**** ConnectionInfo.DBServer          :: "+ci.getDbServer());
    	System.out.println("**** ConnectionInfo.DBDriver          :: "+ci.getDbDriver());
    	System.out.println("**** ConnectionInfo.DBLogin           :: "+ci.getDbLogin());
    	System.out.println("**** ConnectionInfo.DBPassword        :: "+ci.getDbPassword());
    	Connection conn = ci.getConnection();
    	if( conn != null ) {
    		try {
    		conn.close();
    		} catch (Exception e) {
    			System.out.println("General Exception thrown!! e.getMessage() = "+e.getMessage());
    		}
    	} else {
    		conn = null;
    	}
    	System.out.println("Done Testing SMPP Project ConnectionInfo Class.");
    }
    
    // [end] main method
}
