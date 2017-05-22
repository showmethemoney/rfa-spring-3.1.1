package com.metrics.reuters;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metrics.reuters.client.ItemManager;
import com.metrics.reuters.client.LoginClient;
import com.reuters.rfa.common.Context;
import com.reuters.rfa.common.EventQueue;
import com.reuters.rfa.common.EventSource;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.session.Session;
import com.reuters.rfa.session.omm.OMMConsumer;

public class RobustFoundationAPI
{
	protected static final Logger logger = LoggerFactory.getLogger( RobustFoundationAPI.class );

	public static final String NAMED_EVENT_QUEUE = "defaultQueue";
	public static final String NAMED_OMM_CONSUMER = "defaultConsumer";	
	public static final String NAMED_HANDLE_CACHE = "handles";
	
	private EventQueue eventQueue = null;
	private Session session = null;
	private OMMConsumer ommConsumer = null;
	private OMMEncoder encoder = null;
	private OMMPool pool = null;

	private LoginClient loginClient = null;
	private ItemManager itemManager = null;

	private String sessionName = null;
	private String serviceName = null;
	private String userName = null;
	private String fieldDictionaryFilename = null;
	private String enumDictionaryFilename = null;
	private String feedConfigFilename = null;	
	
	public RobustFoundationAPI() {
	}

	public RobustFoundationAPI(String sessionName, String serviceName, String userName, String fieldDictionaryFilename, String enumDictionaryFilename,
	        String feedConfigFilename) {
		this.sessionName = sessionName;
		this.serviceName = serviceName;
		this.userName = userName;
		this.fieldDictionaryFilename = fieldDictionaryFilename;
		this.enumDictionaryFilename = enumDictionaryFilename;
		this.feedConfigFilename = feedConfigFilename;

		init();
		login();
	}

	public void cleanup() {
		eventQueue.deactivate();

		if (null != itemManager)
			itemManager.closeRequest();

		if (null != loginClient)
			loginClient.closeRequest();

		eventQueue.destroy();

		if (null != ommConsumer)
			ommConsumer.destroy();

		session.release();

		Context.uninitialize();
	}

	public void dispatch() {
		try {
			while (true) {
				eventQueue.dispatch( 1000 );
			}
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}
	}

	// This method is called when the login was not successful
	// The application exits
	public void loginFailure() {
		logger.info( "Login has been denied / rejected / closed" );
		logger.info( "Preparing to clean up and exiting" );
		loginClient = null;

		cleanup();
	}

	protected void init() {
		try {
			// 1. Initialize context
			logger.info( "Initialize context" );
			Context.initialize();

			Preferences.importPreferences( new DataInputStream( new FileInputStream( feedConfigFilename ) ) );

			// 2. Create an Event Queue
			logger.info( "Create an Event Queue" );
			eventQueue = EventQueue.create( NAMED_EVENT_QUEUE );

			// 3. Acquire a Session
			logger.info( "Acquire a Session" );
			session = Session.acquire( sessionName );
			if (null == session) {
				logger.info( "Could not acquire session." );
				throw new RuntimeException();
			}

			// 4. Create an OMMConsumer event source
			logger.info( "Create an OMMConsumer event source" );
			ommConsumer = (OMMConsumer) session.createEventSource( EventSource.OMM_CONSUMER, NAMED_OMM_CONSUMER, true );

			GenericOMMParser.initializeDictionary( fieldDictionaryFilename, enumDictionaryFilename );

			// Create a OMMPool.
			logger.info( "Create a OMMPool." );
			pool = OMMPool.create();

			logger.info( "Create an OMMEncoder" );
			// Create an OMMEncoder
			encoder = pool.acquireEncoder();
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );

			cleanup();
		}
	}

	protected void login() {
		loginClient = new LoginClient( this );

		loginClient.sendRequest();
	}

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}

	public ItemManager getItemManager() {
		return itemManager;
	}

	public void setItemManager(ItemManager itemManager) {
		this.itemManager = itemManager;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public OMMPool getPool() {
		return pool;
	}

	public void setPool(OMMPool pool) {
		this.pool = pool;
	}

	public OMMConsumer getOmmConsumer() {
		return ommConsumer;
	}

	public void setOmmConsumer(OMMConsumer ommConsumer) {
		this.ommConsumer = ommConsumer;
	}

	public EventQueue getEventQueue() {
		return eventQueue;
	}

	public void setEventQueue(EventQueue eventQueue) {
		this.eventQueue = eventQueue;
	}

	public LoginClient getLoginClient() {
		return loginClient;
	}

	public void setLoginClient(LoginClient loginClient) {
		this.loginClient = loginClient;
	}
}
