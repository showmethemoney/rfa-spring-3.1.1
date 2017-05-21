package com.metrics.reuters.client;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metrics.reuters.RobustFoundationAPI;
import com.metrics.reuters.event.LoginEvent;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.omm.OMMElementList;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.rdm.RDMUser;
import com.reuters.rfa.session.omm.OMMItemIntSpec;

public class LoginClient
{
	protected static final Logger logger = Logger.getLogger( LoginClient.class );
	private RobustFoundationAPI instance = null;
	private Handle handler = null;

	private String application = "256";
	private String position = "1.1.1.1/net";

	public LoginClient() {
	}

	public LoginClient(RobustFoundationAPI instance) {
		this.instance = instance;
	}

	public void sendRequest() {
		OMMItemIntSpec spec = new OMMItemIntSpec();
		spec.setMsg( encodeLogin( instance.getUserName(), RDMUser.NameType.USER_NAME, OMMMsg.Indication.REFRESH ) );

		LoginEvent event = new LoginEvent( instance );

		handler = instance.getOmmConsumer().registerClient( instance.getEventQueue(), spec, event, null );

		// ?????
		// if (null == handler)
		// _loginInfo = new LoginInfo();
		//
		// _loginInfo.setHandle( _loginHandle );
	}

	private OMMMsg encodeLogin(String userName, short nameType, int indication) {
		OMMMsg result = null;

		try {
			position = InetAddress.getLocalHost().getHostAddress() + "/" + InetAddress.getLocalHost().getHostName();

			OMMEncoder encoder = instance.getPool().acquireEncoder();

			encoder.initialize( OMMTypes.MSG, 1000 );

			OMMMsg msg = instance.getPool().acquireMsg();

			msg.setMsgType( OMMMsg.MsgType.REQUEST );
			msg.setMsgModelType( RDMMsgTypes.LOGIN );
			msg.setAttribInfo( null, userName, nameType );

			if (indication != 0)
				msg.setIndicationFlags( indication );

			encoder.encodeMsgInit( msg, OMMTypes.ELEMENT_LIST, OMMTypes.NO_DATA );
			encoder.encodeElementListInit( OMMElementList.HAS_STANDARD_DATA, (short) 0, (short) 0 );
			encoder.encodeElementEntryInit( RDMUser.Attrib.ApplicationId, OMMTypes.ASCII_STRING );
			encoder.encodeString( application, OMMTypes.ASCII_STRING );
			encoder.encodeElementEntryInit( RDMUser.Attrib.Position, OMMTypes.ASCII_STRING );
			encoder.encodeString( position, OMMTypes.ASCII_STRING );
			encoder.encodeElementEntryInit( RDMUser.Attrib.Role, OMMTypes.UINT );
			encoder.encodeUInt( RDMUser.Role.CONSUMER );
			encoder.encodeElementEntryInit( RDMUser.Attrib.SupportPauseResume, OMMTypes.UINT );
			encoder.encodeUInt( 1 );

			encoder.encodeAggregateComplete();

			result = (OMMMsg) encoder.getEncodedObject();
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}

		return result;
	}

	public void closeRequest() {
		if (null != handler) {
			instance.getOmmConsumer().unregisterClient( handler );
			handler = null;
		}
	}

	public Handle getHandler() {
		return handler;
	}

	public void setHandler(Handle handler) {
		this.handler = handler;
	}
}
