package com.metrics.reuters.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metrics.reuters.GenericOMMParser;
import com.metrics.reuters.RobustFoundationAPI;
import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMState;
import com.reuters.rfa.session.omm.OMMItemEvent;

public class LoginEvent implements Client
{
	protected static final Logger logger = LoggerFactory.getLogger( LoginEvent.class );
	private RobustFoundationAPI instance = null;
	
	public LoginEvent() {}
	
	public LoginEvent(RobustFoundationAPI instance) {
		this.instance = instance;
	}
	
	public void processEvent(Event event) {
		if (event.getType() == Event.COMPLETION_EVENT) {
			logger.info( "Receive a COMPLETION_EVENT, {}", event.getHandle() );
			return;
		}

		logger.info( "processEvent: Received Login Response " );

		OMMItemEvent ie = (OMMItemEvent) event;
		OMMMsg respMsg = ie.getMsg();
		GenericOMMParser parser = new GenericOMMParser();
		
		// The login is unsuccessful, RFA forwards the message from the network
		if (respMsg.isFinal()) {
			logger.info( "Login Response message is final." );
			
			parser.parse( respMsg );
			instance.loginFailure();
			return;
		}

		// The login is successful, RFA forwards the message from the network
		if ((respMsg.getMsgType() == OMMMsg.MsgType.STATUS_RESP) && (respMsg.has( OMMMsg.HAS_STATE ))
		        && (respMsg.getState().getStreamState() == OMMState.Stream.OPEN) && (respMsg.getState().getDataState() == OMMState.Data.OK)) {

			logger.info( "Received Login STATUS OK Response" );
			parser.parse( respMsg );
//			instance.processLogin();
		} else {// This message is sent by RFA indicating that RFA is processing the login

			logger.info( "Received Login Response - {}", OMMMsg.MsgType.toString( respMsg.getMsgType() ) );
			parser.parse( respMsg );
		}
	}
}
