package com.metrics.reuters.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.metrics.reuters.GenericOMMParser;
import com.metrics.reuters.RobustFoundationAPI;
import com.metrics.reuters.bean.ItemIntSpec;
import com.metrics.reuters.event.ItemEvent;
import com.metrics.reuters.event.ItemMapEvent;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.rdm.RDMInstrument;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.session.omm.OMMItemIntSpec;

import net.sf.ehcache.Ehcache;

/**
 *  1. use sendRequest(Collection<String> identifiers, Collection<String> fields)
 *  or
 *  2. use sendRequest(List<ItemIntSpec<String, List<String>> itemIntSpecs)
 */

public class ItemManager
{
	protected static final Logger logger = LoggerFactory.getLogger( ItemManager.class );
	private RobustFoundationAPI instance = null;
	private GenericOMMParser parser = null;
	private CacheManager cacheManager = null;

	public ItemManager() {}

	public ItemManager(RobustFoundationAPI instance) {
		this.instance = instance;
	}

	/**
	 * send request
	 */
	public void sendRequest(Collection<String> identifiers, Collection<String> fields) {
		logger.info( "SendRequest: Sending item requests" );

		closeRequest();

		send( identifiers, fields );
	}

	/**
	 * use ItemIntSpec object
	 * 
	 * send request
	 */
	public void sendRequest(List<ItemIntSpec> itemIntSpecs) {
		logger.info( "SendRequest: Sending item requests" );

		closeRequest();

		// transform to Map<String, ItemIntSpec> for OMMMsg
		Map<String, ItemIntSpec> itemIntSpecMap = new HashMap<String, ItemIntSpec>();
		for (ItemIntSpec itemIntSpec : itemIntSpecs) {
			itemIntSpecMap.put( itemIntSpec.getIdentifiy(), itemIntSpec );
		}

		send( itemIntSpecMap );
	}

	public boolean unregisterClient(Handle handle) {
		boolean result = false;

		try {
			instance.getOmmConsumer().unregisterClient( handle );
			result = true;
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}

		return result;
	}

	public void closeRequest() {
		try {
			Cache cache = cacheManager.getCache( RobustFoundationAPI.NAMED_HANDLE_CACHE );
			Ehcache handlesCache = (Ehcache) cache.getNativeCache();

			if (0 != handlesCache.getSize()) {
				// unregister handle
				Iterator<Handle> iter = (Iterator<Handle>) handlesCache.getKeys().iterator();

				Handle handle = null;
				while (iter.hasNext()) {
					handle = iter.next();
					unregisterClient( handle );
				}

				// spring cache implements
				cache.clear();
			}
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}
	}

	protected void send(Collection<String> identifiers, Collection<String> fields) {
		String serviceName = instance.getServiceName();
		Cache cache = cacheManager.getCache( RobustFoundationAPI.NAMED_HANDLE_CACHE );
		String itemName = null;
		Iterator<String> iterator = identifiers.iterator();

		// register for each item
		while (iterator.hasNext()) {
			itemName = iterator.next();
			OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec();

			// Preparing item request message
			OMMPool pool = instance.getPool();
			OMMMsg ommmsg = pool.acquireMsg();

			ommmsg.setMsgType( OMMMsg.MsgType.REQUEST );
			ommmsg.setMsgModelType( RDMMsgTypes.MARKET_PRICE );
			ommmsg.setIndicationFlags( OMMMsg.Indication.REFRESH );
			ommmsg.setPriority( (byte) 1, 1 );

			// Setting OMMMsg with negotiated version info from login handle
			if (null != instance.getLoginClient().getHandler()) {
				ommmsg.setAssociatedMetaInfo( instance.getLoginClient().getHandler() );
			}

			logger.info( "Subscribing Identifier: " + itemName );

			ommmsg.setAttribInfo( serviceName, itemName, RDMInstrument.NameType.RIC );

			// Set the message into interest spec
			ommItemIntSpec.setMsg( ommmsg );

			ItemEvent event = new ItemEvent( instance, identifiers, fields, parser, cache );

			Handle itemHandle = instance.getOmmConsumer().registerClient( instance.getEventQueue(), ommItemIntSpec, event, null );
			// put handle and itemname in cache
			cache.put( itemHandle, itemName );

			// itemHandles.add( itemHandle );

			pool.releaseMsg( ommmsg );
		}
	}

	protected void send(Map<String, ItemIntSpec> itemIntSpecMap) {
		String serviceName = instance.getServiceName();

		OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec();

		// Preparing item request message
		OMMPool pool = instance.getPool();
		OMMMsg ommmsg = pool.acquireMsg();

		ommmsg.setMsgType( OMMMsg.MsgType.REQUEST );
		ommmsg.setMsgModelType( RDMMsgTypes.MARKET_PRICE );
		ommmsg.setIndicationFlags( OMMMsg.Indication.REFRESH );
		ommmsg.setPriority( (byte) 1, 1 );

		// Setting OMMMsg with negotiated version info from login handle
		if (null != instance.getLoginClient().getHandler()) {
			ommmsg.setAssociatedMetaInfo( instance.getLoginClient().getHandler() );
		}

		// register for each item
		Set<String> intSpecKeys = itemIntSpecMap.keySet();
		Iterator<String> iterator = intSpecKeys.iterator();

		String itemName = null;

		Cache cache = cacheManager.getCache( RobustFoundationAPI.NAMED_HANDLE_CACHE );

		while (iterator.hasNext()) {
			itemName = iterator.next();
			logger.info( "Subscribing Identifier: " + itemName );

			ommmsg.setAttribInfo( serviceName, itemName, RDMInstrument.NameType.RIC );

			// Set the message into interest spec
			ommItemIntSpec.setMsg( ommmsg );

			ItemMapEvent event = new ItemMapEvent( instance, itemIntSpecMap, cache, parser );

			Handle itemHandle = instance.getOmmConsumer().registerClient( instance.getEventQueue(), ommItemIntSpec, event, null );

			// put handle and itemname in cache
			cache.put( itemHandle, itemName );
		}

		pool.releaseMsg( ommmsg );
	}

	public RobustFoundationAPI getInstance() {
		return instance;
	}

	public void setInstance(RobustFoundationAPI instance) {
		this.instance = instance;
	}

	public GenericOMMParser getParser() {
		return parser;
	}

	public void setParser(GenericOMMParser parser) {
		this.parser = parser;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
}
