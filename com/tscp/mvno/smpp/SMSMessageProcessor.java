package com.tscp.mvno.smpp;

import ie.omk.smpp.Connection;
import ie.omk.smpp.message.BindResp;
import ie.omk.smpp.message.SMPPResponse;
import ie.omk.smpp.message.SubmitSM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.tscp.mvno.smpp.db.ConnectionInfo;
import com.tscp.mvno.smpp.db.SPArgs;
import com.tscp.mvno.smpp.db.StoredProc;

public class SMSMessageProcessor {
	public static final int MESSAGE_TYPE_TEST_SMS		= 0;
	public static final int	MESSAGE_TYPE_PMT_MADE		= 1;
	public static final int MESSAGE_TYPE_BAL_ALERT		= 2;
	public static final int MESSAGE_TYPE_TEXT_ALERT 	= 3;
	public static final int MESSAGE_TYPE_DID_ALERT_A	= 44;
	public static final int MESSAGE_TYPE_DID_ALERT_B	= 45;
	public static final int MESSAGE_TYPE_LOSE_MDN		= 5;
	public static final int MESSAGE_TYPE_JUL09_BLAST	= 6;
	public static final int MESSAGE_TYPE_SENDFAILED		= 7;
	public static final int MESSAGE_TYPE_FLEXMSGLIST	= 10;
	public static final int MESSAGE_TYPE_ACTIVATION  	= 100;	
	public static final int MESSAGE_TYPE_PROM_CAPABILITY = 110;
	
	private Connection 		smppConnection;
	private ConnectionInfo	dbConnection;
	private Logger			log;
	private String			logConfigFile = "SMPPMessag.logger";
		
	public SMSMessageProcessor() {
	}
	
	public void init() throws Exception{
		try{
		  smppConnection = SMPPConnectionUtil.makeConnection();
		  dbConnection = new ConnectionInfo();
		}
		catch(Exception e){
			e.printStackTrace();
	        throw e;
		}
		
		try{
		  DOMConfigurator.configure("/apps/home/appadmin/telscape/conf/SMPPMessaging.logger");
		}
		catch(Exception e){
			e.printStackTrace();
		}

		log = Logger.getLogger(logConfigFile);
	}
	
	public void processMessage(List<SubmitSM> smsList, int iMessageType) throws Exception{
	
		//log.info("*********** Starting message process :: "+Integer.toString(iMessageType)+"**************");
			
		HashMap<String,String> mdnSocList = new HashMap<String, String>();
		
		String messageId = null;
				
		int messageSentCounter = 0;		
	
		for( int j = 0; j < smsList.size(); j++ ) {
			System.out.println("Preparing message for MDN: "+smsList.get(j).getDestination().getAddress()+" with Message: "+smsList.get(j).getMessageText());
			log.info("Preparing message for MDN: "+smsList.get(j).getDestination().getAddress()+" with Message: "+smsList.get(j).getMessageText());
			String messageBlockingSOC = "";
			
			try {
				SMPPConnectionUtil.bind(smppConnection);
				messageId = sendRequest(smsList.get(j).getDestination().getAddress(),smsList.get(j).getMessageText());
			    System.out.println("Message was sent out successfully to: " + smsList.get(j).getDestination().getAddress());
			}
			catch(Exception e){
				System.out.println("Exception occured in processMessage(): " + e.getMessage());
				if( j == smsList.size() - 1 ) {
					SMPPConnectionUtil.unbind(smppConnection);
					throw e;
				}	
			}
			
			if( messageId != null && messageId.trim().length() > 0 && messageBlockingSOC != null && messageBlockingSOC.trim().length() > 0 ) {
				log.info("Attempting to reinstate "+messageBlockingSOC+" on MDN "+smsList.get(j).getDestination().getAddress());
				MessageSupport.modifySoc(smsList.get(j).getDestination().getAddress(), messageBlockingSOC, MessageSupport.ACTION_ADD);
				log.info("Adding of "+messageBlockingSOC+" completed for MDN "+smsList.get(j).getDestination().getAddress());
			}
		
			//if( messageSentCounter == 100 ) {
			//	SMPPConnectionUtil.unbind(smppConnection);
			//	messageSentCounter = 0;
			//}
			//once we're done traversing the list of pending SMS messages, we want to unbind our connection.
			if( j == smsList.size() - 1 ) {
				SMPPConnectionUtil.unbind(smppConnection);
			}
			++messageSentCounter;			
		}
	}
	
	public void releaseConnections() {
		
		SMPPConnectionUtil.releaseConnection(smppConnection);
		
		if( dbConnection != null ) {
			try{ 
				dbConnection.releaseConnection();		
			} catch( Exception e ) {
				System.out.println("Error closing dbConnection "+e.getMessage());
			} finally {
				dbConnection = null;
			}
		}
	}	
	
	
	private String sendRequest(String iPhoneNumber, String iMessage) throws Exception{
		
		log.info("**** send request to " + iPhoneNumber);
		String retValue = null;
		try {
//			SMPPRequest smppRequest;
			ie.omk.smpp.message.SubmitSM shortMsg = new SubmitSM();
			ie.omk.smpp.Address destAddress = new ie.omk.smpp.Address();
			destAddress.setAddress(iPhoneNumber);
			
			ie.omk.smpp.Address sendAddress = new ie.omk.smpp.Address();
			sendAddress.setTON(0);
			sendAddress.setNPI(0);
			sendAddress.setAddress(SMPPConnectionUtil.getShortCode());
			shortMsg.setDestination(destAddress);
			shortMsg.setSource(sendAddress);
			shortMsg.setMessageText(iMessage);
//			smppRequest = shortMsg;
			smppConnection.autoAckMessages(true);
			System.out.println("------ SMPPRequest -------");
			//System.out.println("SMPPRequest Source Address   = "+shortMsg.getSource().getAddress());
			//System.out.println("SMPPRequest Message Text     = "+shortMsg.getMessageText());
			System.out.println("SMPPRequest Dest Address     = "+shortMsg.getDestination().getAddress());
			//log.info("SMPPRequest properties ");
			//log.info("SMPPRequest Source Address   = "+shortMsg.getSource().getAddress());
			//log.info("SMPPRequest Message Text     = "+shortMsg.getMessageText());
			//log.info("SMPPRequest Dest Address     = "+shortMsg.getDestination().getAddress());			
			//System.out.println("smppConnection.isBound() = " + smppConnection.isBound());
			//System.out.println("smppConnection.BOUND = "+smppConnection.BOUND);
			SMPPResponse smppResponse = smppConnection.sendRequest(shortMsg);
			
//			if( smppResponse.getCommandStatus() == SMPPResponse.)
			if( smppResponse != null ) {

				System.out.println("------ SMPPResponse -------");
				//System.out.println("SMPPResponse Message         = "+smppResponse.getMessage());
				System.out.println("SMPPResponse MessageID       = "+smppResponse.getMessageId());
				//System.out.println("SMPPResponse MessageStatus   = "+smppResponse.getMessageStatus());
				//System.out.println("SMPPResponse MessageText     = "+smppResponse.getMessageText());
				//log.info("SMPPResponse Message         = "+smppResponse.getMessage());
				//log.info("SMPPResponse MessageID       = "+smppResponse.getMessageId());
				//log.info("SMPPResponse MessageStatus   = "+smppResponse.getMessageStatus());
				//log.info("SMPPResponse MessageText     = "+smppResponse.getMessageText());
				
				if( smppResponse.getMessageId() == null || smppResponse.getMessageId().trim().length() == 0 ) {
					retValue = smppResponse.getMessageId();
				} else {
					retValue = smppResponse.getMessageId();
				}
			} else {
				System.out.println("SMPPResponse is null...");
				log.warn("SMPPResponse is null!!!");
			}
		} catch( Exception e ) {
			log.error("!!Error sending request!! due to:  " + e.getMessage());
			System.out.println("!!Error sending request!! due to:  " + e.getMessage());
			throw e;
		}
		return retValue;
	}
	
	private List<SubmitSM> getMessageList(int iMessageType) throws Exception {
		
		List<SubmitSM> messageList = Collections.emptyList();
		
		boolean addMoreRecords = true;

		switch ( iMessageType ) {
		case SMSMessageProcessor.MESSAGE_TYPE_PROM_CAPABILITY:
			messageList = getPromCapabilityMessageList();
			break; 	
		/*case SMSMessageProcessor.MESSAGE_TYPE_ACTIVATION:
			getActivationMessageList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_TEST_SMS:
			getTestMessageList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_BAL_ALERT:
			getBalanceAlertMessageList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_PMT_MADE:
			getPaymentMadeMessageList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_TEXT_ALERT:
			getBalanceTextAlert(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_DID_ALERT_A:
			getDIDNotifcationA(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_DID_ALERT_B:
			getDIDNotificationB(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_LOSE_MDN:
			getLoseMDNMessageList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_JUL09_BLAST:
			getRateChangeBlastList(messageList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_SENDFAILED:
			 getFailedSMSRecords(messageList,mdnSocList);
			break;
		case SMSMessageProcessor.MESSAGE_TYPE_FLEXMSGLIST:
			getNewFLEXMessageList(messageList);
			break;
		*/default:
			messageList = null;
		}		
		System.out.println("SMS List returned with "+messageList.size()+" elements.");
		log.info("SMS List returned with "+messageList.size()+" elements.");
		
		return messageList;
	}
	
	
	private List<SubmitSM> generateTestMessage( String iMdn, String iMessage ) {
		List<SubmitSM> workingSet = new ArrayList<SubmitSM>();
		return workingSet;
	}
	
	private List<SubmitSM> getTestMessageList( List<SubmitSM> workingSet ) {
		//Manual Tester used to avoid connecting to the DB and retrieving live data.
		boolean addMoreRecords = true;
		String input = "";
//		BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
		int maxTestCases = 3;
		int i = 0;
//		while(MainHelper.addMoreInformation("Add Messages for testing?", br)) {
		while( addMoreRecords ) {
			SubmitSM shortMessage = new SubmitSM();
			ie.omk.smpp.Address address = new ie.omk.smpp.Address();
//			address.setAddress(MainHelper.populateRequiredField("MDN", br, address.getAddress()));
//			shortMessage.setMessageText(MainHelper.populateRequiredField("Text MSG(160 Chrs)", br, shortMessage.getMessageText()));
			switch( i ) {
			case 0:
				address.setAddress("6268487491");  //WOMS Support Handset
				break;
			case 1:
				address.setAddress("9097440888");	//Dan's AT&T Cell Phone 
				break;
			case 2:
				address.setAddress("3232163660");	//Tscp IT test handset ESN 02209279799
				break;
			case 3:
				//address.setAddress("6262722581");  //Paulina's HandSet
				break;
			case 4:
				//address.setAddress("6263672931"); //Janet Uribe's cell phone
				break;
			default:
				address.setAddress("6268487491");  //WOMS Support Handset
			}
//			address.setAddress("6268487491");  //WOMS Support Handset
//			address.setAddress("6262722581");  //Paulina's HandSet
			//address.setAddress("9135114036");
//			address.setAddress("9135034699");
			shortMessage.setDestination(address);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String timeStamp = sdf.format(new java.util.Date());
			shortMessage.setMessageText("Testing SMS Application at " + timeStamp );
//			shortMessage.setMessageText("Reciba llamadas desde Mexico sin limite. Su familia hace una llamada local en su pais que suena aqui en su telefono. Para mas informacion llame al 877 771-7736 ");
//			shortMessage.setMessageText("Reciba llamadas desde Mexico SIN LIMITE. Su familia hace una llamada local en Mexico que suena aqui en su casa. Para mas informacion llame al 877 771-7736");
//			shortMessage.setMessageText("Telscape MSG: Reciba llamadas desde Mexico SIN LIMITE. Su familia hace una llamada local en Mexico que suena aqui en su casa. Para detalles 877 771-7736");
//			shortMessage.setMessageText("Telscape Free MSG: Receive UNLIMITED calls from Mexico. Your family dials a local number in Mexico that connects to your home phone. For more information, call 877 771-7736");
//			shortMessage.setMessageText("Telscape MSG: Receive UNLIMITED calls from Mexico. Your family dials a local number in Mexico that rings at home in the US. For details, call 877 771-7736");
			workingSet.add(shortMessage);
			++i;
			if( i == maxTestCases) {
				addMoreRecords = false;
			}
		}
		return workingSet;
	}
			
	private List<SubmitSM> getPromCapabilityMessageList() throws Exception{ 
		StoredProc sp = new StoredProc(dbConnection.getConnection());
		SPArgs spargs = new SPArgs();
		spargs.put("sp", "tscp_woms_pkg.sp_get_marketing_sms");// at report@mvnot
		ResultSet rs = null;
		List<SubmitSM> messageList = new ArrayList<SubmitSM>(); 
		
		try {
			rs = sp.exec(spargs,1);
			System.out.println("----- Elements retirved -----");
			
			while( rs.next() ) {				
				System.out.println("EXTERNAL_ID = " + rs.getString("EXTERNAL_ID"));
				//System.out.println("TEXT_MESSAGE = " + rs.getString("SMS_MSG"));
				
				SubmitSM shortMessage = new SubmitSM();
				ie.omk.smpp.Address address = new ie.omk.smpp.Address();
				//address.setAddress(rs.getString("EXTERNAL_ID"));
                            address.setAddress("6000000000");

				
                            shortMessage.setDestination(address);
				shortMessage.setMessageText(rs.getString("SMS_MSG"));
				messageList.add(shortMessage);
			}
		} catch (Exception e ) {
			e.printStackTrace();
			System.out.println("Error encountered when getting message list..."+e.getMessage());
			log.error("Error encountered when getting payment made list..."+e.getMessage());
			throw e;
		} finally {
			sp.close(rs);
			dbConnection.releaseConnection();
		}
		return messageList;
	}
	
	private List<SubmitSM> getActivationMessageList( List<SubmitSM> workingSet ) { 
		StoredProc sp = new StoredProc(dbConnection.getConnection());
		SPArgs spargs = new SPArgs();
		spargs.put("sp", "etc_tscp_woms_pkg.sp_new_activations_text_alert");
		ResultSet rs = sp.exec(spargs,1);
		
		try {
								
			while( rs.next() ) {
				System.out.println("EXTERNAL_ID = " + rs.getString("EXTERNAL_ID"));
				System.out.println("TEXT_MESSAGE = " + rs.getString("TEXT_MESSAGE"));
				
				SubmitSM shortMessage = new SubmitSM();
				ie.omk.smpp.Address address = new ie.omk.smpp.Address();
				address.setAddress(rs.getString("EXTERNAL_ID"));
				shortMessage.setDestination(address);
				shortMessage.setMessageText(rs.getString("TEXT_MESSAGE"));
				workingSet.add(shortMessage);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			System.out.println("Error encountered when getting payment made list..."+e.getMessage());
			log.error("Error encountered when getting payment made list..."+e.getMessage());
		} finally {
			sp.close(rs);
			dbConnection.releaseConnection();
		}
		return workingSet;
	}
	
	
	private void insertMessageRecord(String iTN, String iSOC, String iMessageId, String iMessageText) {
		StoredProc sp = new StoredProc(dbConnection.getConnection());
		SPArgs spargs = new SPArgs();
		spargs.put("sp", "DT_PUT_SMSRECORD");
		spargs.put("arg1", iTN);
		spargs.put("arg2", iSOC);
		spargs.put("arg3", iMessageId);
		spargs.put("arg4", iMessageText);
		ResultSet rs = sp.exec(spargs,1);
		try {
			rs.next();
			
		} catch( SQLException sql_ex ) {
			log.warn("Error inserting TN "+iTN+" SOC "+iSOC+" MESSAGE "+iMessageId+" :::: "+sql_ex.getMessage());
		} finally {
			sp.close(rs);
			dbConnection.releaseConnection();
		}
	}
	
	private void getFailedSMSRecords(List<SubmitSM> messageList, HashMap<String,String> mdnSocList) {
		String spName = "DT_GET_SMSFAILED";
		StoredProc sp = new StoredProc(dbConnection.getConnection());
		SPArgs spargs = new SPArgs();
		spargs.put("sp",spName);
		ResultSet rs = sp.exec(spargs,1);
		try {
			while( rs.next() ) {
				SubmitSM shortMessage = new SubmitSM();
				ie.omk.smpp.Address address = new ie.omk.smpp.Address();
				address.setAddress(rs.getString("MDN"));
				shortMessage.setDestination(address);
				shortMessage.setMessageText(rs.getString("TEXT"));
				messageList.add(shortMessage);
				mdnSocList.put(rs.getString("MDN"), rs.getString("SOC"));
			}
		} catch( SQLException sql_ex ) {
			log.warn("getFailedSMSRecords threw SQL Exception "+sql_ex.getMessage(),sql_ex);
		} finally {
			sp.close(rs);
			dbConnection.releaseConnection();
		}
	}
		
    public static void main(String[] args) { 
    	
    	System.out.println("Start SMSMessageProcessor...");
    	   
    	List<SubmitSM> msgList = null;
    	
    	int messageType = MESSAGE_TYPE_PROM_CAPABILITY;
    	
    	try {
            
    		SMSMessageProcessor messageProcessor = new SMSMessageProcessor();
    	
    		System.out.println("********* Initialize the connections and logger *********");
    		
    		messageProcessor.init();
    		
    		System.out.println("******** Get message list ***********");
    		
    		msgList = messageProcessor.getMessageList(messageType);
    	    		    	      	
    	    System.out.println("******** Process the messages ********");
    	
    	    messageProcessor.processMessage(msgList, messageType);

    	    System.out.println("******** Close Connections **********");
    	    
    	    messageProcessor.releaseConnections();   
    	}
    	catch(Throwable t){
    		System.out.println("Exit the process due to an exception occured : " + t.getMessage());
    	    System.exit(1);	
    	}
    	System.out.println("Done SMPP MessageProcessor.");
    	System.exit(0);
    }
}
