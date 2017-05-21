package com.metrics.reuters.event;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

import com.metrics.reuters.GenericOMMParser;
import com.metrics.reuters.RobustFoundationAPI;
import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.session.omm.OMMItemEvent;

import net.sf.ehcache.Ehcache;

public class ItemEvent implements Client
{
	protected static final Logger logger = LoggerFactory.getLogger( ItemEvent.class );
	private RobustFoundationAPI instance = null;
	private Collection<String> identifiers = null;
	private Collection<String> fields = null;
	private GenericOMMParser parser = null;
	private Cache cache = null;

	public ItemEvent() {
	}

	public ItemEvent(RobustFoundationAPI instance, Collection<String> identifiers, Collection<String> fields, GenericOMMParser parser, Cache cache) {
		this.instance = instance;
		this.identifiers = identifiers;
		this.fields = fields;
		this.parser = parser;
		this.cache = cache;
	}

	/**
	 * MESSAGE Msg Type: MsgType.UPDATE_RESP Msg Model Type: MARKET_PRICE Indication Flags: DO_NOT_CONFLATE Hint Flags: HAS_ATTRIB_INFO | HAS_RESP_TYPE_NUM |
	 * HAS_SEQ_NUM SeqNum: 22782 RespTypeNum: 0 (UNSPECIFIED) AttribInfo ServiceName: API_ELEKTRON_EPD_RSSL ServiceId: 2115 Name: JPY= NameType: 1 (RIC)
	 * Payload: 13 bytes FIELD_LIST FIELD_ENTRY 114/BID_NET_CH: -0.28 FIELD_ENTRY 372/IRGPRC: -0.25
	 */
	public void processEvent(Event event) {
		Ehcache ehcache = (Ehcache) cache.getNativeCache();
		logger.debug( "event type : {} - size : {}", event.getType(), ehcache.getSize() );

		if (event.getType() == Event.COMPLETION_EVENT) {
			return;
		}

		// check for an event type; it should be item event.
		if (event.getType() != Event.OMM_ITEM_EVENT) {
			instance.cleanup();
			return;
		}

		OMMItemEvent ie = (OMMItemEvent) event;
		OMMMsg respMsg = ie.getMsg();
		
		ValueWrapper wrapper = cache.get( event.getHandle() );

		if (null != wrapper) {
			String eventItemName = (String) wrapper.get();

			logger.debug( "event item name : {}", eventItemName );

			if (identifiers.contains( eventItemName )) {
				parser.setFields( fields );
				parser.setItemName( eventItemName );
				parser.parse( respMsg );
			}  
		} 
	}
}

