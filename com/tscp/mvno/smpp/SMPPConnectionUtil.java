package com.tscp.mvno.smpp;

import ie.omk.smpp.Connection;
import ie.omk.smpp.message.BindResp;
import ie.omk.smpp.net.TcpLink;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Class designed to open the connection using Sprint as the SMSC
 *
 */

public class SMPPConnectionUtil {
 
public static String inputProperties = "client.properties";
			
	static String		sprintSmscSite;
	static int			sprintSmscPort;		
	static String		telscapeUserName;
	static String		telscapePassword;
	static String		systemType;
	static int			maximumMessageCount;
	static String 		shortCode;
		
	static  {
		init();
	}
			
	private static void init() {
		try {
			Properties props = new Properties();
			//ClassLoader cl = ConnectionUtil.class.getClassLoader();
			ClassLoader cl = SMPPConnectionUtil.class.getClassLoader();
			InputStream in = cl.getResourceAsStream(inputProperties);
			
			if(in != null)
			   props.load(in);
		   
			sprintSmscSite 		= props.getProperty("SMSC.URL", "http://68.28.216.140/");
		    //sprintSmscSite 		= props.getProperty("SMSC.URL", "http://63.168.232.135/");
		    sprintSmscPort 		= Integer.parseInt(props.getProperty("SMSC.PORT", "16910"));
		   
		    //sprintSmscPort 		= Integer.parseInt(props.getProperty("SMSC.PORT", "80"));
		    telscapeUserName	= props.getProperty("TSCP.USERNAME", "tscp");
		    telscapePassword	= props.getProperty("TSCP.PASSWORD", "tscp2008");
		    systemType			= props.getProperty("SYSTEM.TYPE", "systype");
		    maximumMessageCount	= Integer.parseInt(props.getProperty("MESSAGE.MAXIMUM","100"));
		    shortCode			= props.getProperty("SHORT.CODE", "87276");		    
		} catch( Exception e ) {
			System.out.println("Error loading properties file!! due to: " + e.getMessage());			
		}
	}
	
	public static Connection makeConnection() throws UnknownHostException, Exception{
				
		TcpLink		tcpLink = null;
		Connection 	smppConnection = null;
		
		System.out.println("Connecting SMSC...");
				
		try {		
			tcpLink = new TcpLink(sprintSmscSite,sprintSmscPort);
			System.out.println("TcpLink established to SMSC server: "+sprintSmscSite + " on port: " + sprintSmscPort);
    	} 
		catch(UnknownHostException uhe ) {
			uhe.printStackTrace();
			throw uhe;
		}
    					
		try {
		    smppConnection = new Connection(tcpLink);			    
		    System.out.println("SMPP Connection established. State = " + smppConnection.getState()); //0-UNBOUND, 1-BINDING, 2-BOUND
		}
	    catch(Exception e ) {
	    	releaseConnection(smppConnection);
		    e.printStackTrace();
		    throw e;
	    }		
	   	    
	    return smppConnection;
	
	}  
	
	/**
	 * Binding in SMPP is the same as logging into the remote SMSC.
	 * <p>Important parameters for Connection.bind() are:
	 * <table border=true>
	 * 	<th>Field</th><th>datatype</th>
	 * 	<tr><td>Connection Type</td><td align=center>int</td></tr>
	 * 	<tr><td>System ID</td><td align=center>String</td></tr>
	 * 	<tr><td>Password</td><td align=center>String</td></tr>
	 * 	<tr><td>System Type</td><td align=center>String</td></tr>
	 * </table>
	 * </p>
	 * @return
	 */
	public static boolean bind(Connection smppConnection) throws Exception{
		boolean retValue = false;
		System.out.println("**** Binding SMPP Connection ****");
		try {
//			BindResp response = new BindResp();
			if( !smppConnection.isBound() ) {
								
				BindResp response = null;
				try{
				   response = smppConnection.bind(Connection.TRANSMITTER, telscapeUserName, telscapePassword, systemType);
				}
				catch(Exception e){
					System.out.println("Exception occured while binding, due to "+ e.getMessage());
				}
				if( response != null ) {
					System.out.println("**** Binding response is not null...");
					System.out.println("**** Binding response System ID :: "+response.getSystemId());
					System.out.println("**** Binding response Destination :: "+response.getDestination());
					//log.info("**** Binding response is not null...");
					//log.info("**** Binding response System ID :: "+response.getSystemId());
					//log.info("**** Binding response Destination :: "+response.getDestination());
				}
			} 
			else {
				System.out.println("SMPP Connection is already bound");
			}
			retValue = true;
		} catch ( Exception e ) {
			System.out.println("!!Binding Exception Occurred, due to: " + e.getMessage());
			releaseConnection(smppConnection);
			throw e;
		} 
		return retValue;
	}
	
	public static boolean unbind(Connection smppConnection) { 
		boolean retValue = false;
		try {
			if( smppConnection.isBound() ) {
				System.out.println("Attempting to unbind SMPP Connection");
				smppConnection.unbind();
			} else {
				System.out.println("SMPP Application is unbound or already unbound ");
				return retValue;
			}
			retValue = true;
			System.out.println("SMPP Connection Unbound successfully");
		} catch ( Exception e ) {
			System.out.println("!!UnBinding Exception Occurred, due to: "+ e.getMessage());
		}
		return retValue;
	}
	
	public static void releaseConnection(Connection smppConnection) {
		if( smppConnection != null ) {
			try {
				smppConnection.unbind();
				smppConnection.closeLink();
			} 
			catch (Exception ex) {
				smppConnection = null;
			}
		}	
	}		
	
	public String getTelscapeUserName() {
		return telscapeUserName;
	}
	
	public String getTelscapePassword() {
		return telscapePassword;
	}
	
	public String getSystemType() {
		return systemType;
	}
	
	public int getMaximumMessageCount() {
		return maximumMessageCount;
	}
	
	public static String getShortCode() {
		return shortCode;
	}
			
	public static void main ( String[] args ) {
		System.out.println("Testing ConnectionUtil for SMPP project...");
		
		System.out.println("Building instance of ConnectionUtil");
		
		//ConnectionUtil connection = new ConnectionUtil();
		try {
		  makeConnection();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("ConnectionUtil object established.");
		
		//System.out.println("Config information:");
		//System.out.println("**** ConnectionUtil.getTelscapeUserName()      = "+connection.getTelscapeUserName());
		//System.out.println("**** ConnectionUtil.getTelscapePassword()      = "+connection.getTelscapePassword());
		//System.out.println("**** ConnectionUtil.getSystemType()            = "+connection.getSystemType());
		//System.out.println("**** ConnectionUtil.getMaximumMessageCount()   = "+connection.getMaximumMessageCount());
		
		System.out.println("Done Testing ConnectionUtil for SMPP Project...Exited normally...");
	}
}
