package com.metrics.reuters.event;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

import com.metrics.reuters.GenericOMMParser;
import com.metrics.reuters.RobustFoundationAPI;
import com.metrics.reuters.bean.ItemIntSpec;
import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.session.omm.OMMItemEvent;

public class ItemMapEvent implements Client
{
	protected static final Logger logger = LoggerFactory.getLogger( ItemMapEvent.class );
	private RobustFoundationAPI instance = null;
	private Map<String, ItemIntSpec> itemIntSpecMap = null;
	private GenericOMMParser parser = null;
	private Cache cache = null;
	
	public ItemMapEvent() {}

	public ItemMapEvent(RobustFoundationAPI instance, Map<String, ItemIntSpec> itemIntSpecMap, Cache cache, GenericOMMParser parser) {
		this.instance = instance;
		this.itemIntSpecMap = itemIntSpecMap;
		this.parser = parser;
		this.cache = cache;
	}

	/**
	 * MESSAGE Msg Type: MsgType.UPDATE_RESP Msg Model Type: MARKET_PRICE Indication Flags: DO_NOT_CONFLATE Hint Flags: HAS_ATTRIB_INFO | HAS_RESP_TYPE_NUM |
	 * HAS_SEQ_NUM SeqNum: 22782 RespTypeNum: 0 (UNSPECIFIED) AttribInfo ServiceName: API_ELEKTRON_EPD_RSSL ServiceId: 2115 Name: JPY= NameType: 1 (RIC)
	 * Payload: 13 bytes FIELD_LIST FIELD_ENTRY 114/BID_NET_CH: -0.28 FIELD_ENTRY 372/IRGPRC: -0.25
	 */
	public void processEvent(Event event) {
		logger.info( "event type : {}", event.getType() );

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

			if (itemIntSpecMap.containsKey( eventItemName )) {
				// GenericOMMParser parser = new GenericOMMParser();
				parser.setFields( ((ItemIntSpec) itemIntSpecMap.get( eventItemName )).getFields() );
				parser.setItemName( eventItemName );
				parser.parse( respMsg );
			}
		}
	}
}
