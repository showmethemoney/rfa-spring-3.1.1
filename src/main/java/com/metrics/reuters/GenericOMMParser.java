package com.metrics.reuters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.reuters.rfa.dictionary.DictionaryException;
import com.reuters.rfa.dictionary.FidDef;
import com.reuters.rfa.dictionary.FieldDictionary;
import com.reuters.rfa.omm.OMMAttribInfo;
import com.reuters.rfa.omm.OMMData;
import com.reuters.rfa.omm.OMMEntry;
import com.reuters.rfa.omm.OMMFieldEntry;
import com.reuters.rfa.omm.OMMFieldList;
import com.reuters.rfa.omm.OMMFilterEntry;
import com.reuters.rfa.omm.OMMIterable;
import com.reuters.rfa.omm.OMMMap;
import com.reuters.rfa.omm.OMMMapEntry;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.omm.OMMVector;
import com.reuters.rfa.omm.OMMVectorEntry;

public class GenericOMMParser
{
	protected static final Logger logger = LoggerFactory.getLogger( GenericOMMParser.class );
	private static HashMap<Integer, FieldDictionary> DICTIONARIES = new HashMap<Integer, FieldDictionary>();
	private static FieldDictionary CURRENT_DICTIONARY = null;
	private String itemName = null;
	private Collection<String> fields = null;
	private CacheManager cacheManager = null;

	public GenericOMMParser() {
	}

	public static void initializeDictionary(String fieldDictionaryFilename, String enumDictionaryFilename) throws DictionaryException {
		FieldDictionary dictionary = FieldDictionary.create();
		try {
			FieldDictionary.readRDMFieldDictionary( dictionary, fieldDictionaryFilename );

			FieldDictionary.readEnumTypeDef( dictionary, enumDictionaryFilename );

			initializeDictionary( dictionary );
		} catch (DictionaryException e) {
			throw new DictionaryException( "ERROR: Check if files " + fieldDictionaryFilename + " and " + enumDictionaryFilename + " exist and are readable.",
			        e );
		}
	}

	// This method can be used to initialize a downloaded dictionary
	public synchronized static void initializeDictionary(FieldDictionary dict) {
		int dictId = dict.getDictId();

		if (0 == dictId) {
			dictId = 1; // dictId == 0 is the same as dictId 1
		}

		DICTIONARIES.put( new Integer( dictId ), dict );
	}

	public FieldDictionary getDictionary(int dictId) {

		if (dictId == 0) {
			dictId = 1;
		}

		return (FieldDictionary) DICTIONARIES.get( new Integer( dictId ) );
	}

	public void parse(OMMMsg msg) {
		parseMsg( msg );
	}

	/**
	 * parse msg and print it in a table-nested format to the provided PrintStream
	 */
	protected void parseMsg(OMMMsg msg) {
		logger.debug( "msg.getMsgType() : {} , msg.getDataType() : {}..", new Object[] { msg.getMsgType(), msg.getDataType() } );

		if (msg.getDataType() == OMMTypes.PERMISSION_DATA) {
			logger.warn( "{} is PERMISSION_DATA", itemName );
		}

		if (msg.has( OMMMsg.HAS_ATTRIB_INFO )) {
			OMMAttribInfo ai = msg.getAttribInfo();
			if (ai.has( OMMAttribInfo.HAS_ATTRIB )) {
				parseData( ai.getAttrib() );
			}
		}

		if (msg.getDataType() != OMMTypes.NO_DATA) {
			parseData( msg.getPayload() );
		}
	}

	@SuppressWarnings("rawtypes")
	private void parseAggregate(OMMData data) {
		parseAggregateHeader( data );

		for (Iterator iter = ((OMMIterable) data).iterator(); iter.hasNext();) {
			OMMEntry entry = (OMMEntry) iter.next();
			parseEntry( entry );
		}
	}

	private void parseAggregateHeader(OMMData data) {
		short dataType = data.getType();
		logger.debug( "dataType : {}", dataType );

		switch (dataType) {
		case OMMTypes.FIELD_LIST: {
			// set DICTIONARY to the dictId for this field list
			OMMFieldList fieldList = (OMMFieldList) data;
			int dictId = fieldList.getDictId();
			CURRENT_DICTIONARY = getDictionary( dictId );
		}
			break;
		case OMMTypes.MAP: {
			OMMMap s = (OMMMap) data;
			if (s.has( OMMMap.HAS_SUMMARY_DATA )) {
				parseData( s.getSummaryData() );
			}
		}
			break;
		case OMMTypes.VECTOR: {
			OMMVector s = (OMMVector) data;

			if (s.has( OMMVector.HAS_SUMMARY_DATA )) {

				// //System.out.println("SUMMARY");
				parseData( s.getSummaryData() );
			}
		}
			break;
		}
	}

	private void parseData(OMMData data) {
		logger.debug( "OMMDataType: {}", data.getType() );

		if (data.isBlank()) {
		} else if (OMMTypes.isAggregate( data.getType() )) {
			logger.debug( "OMMTypes is Aggregate" );

			parseAggregate( data );
		} else if (data.getType() == OMMTypes.MSG) {
			parseMsg( (OMMMsg) data );
		}
	}

	private void parseEntry(OMMEntry entry) {
		logger.debug( "entry: " + entry.getType() );

		try {
			switch (entry.getType()) {
			case OMMTypes.FIELD_ENTRY:
				OMMFieldEntry fe = (OMMFieldEntry) entry;

				if (CURRENT_DICTIONARY != null) {
					FidDef fiddef = CURRENT_DICTIONARY.getFidDef( fe.getFieldId() );
					if (fiddef != null) {
						OMMData data = null;
						if (fe.getDataType() == OMMTypes.UNKNOWN) {
							data = fe.getData( fiddef.getOMMType() );
						} else {
							// defined data already has type
							data = fe.getData();
						}

						if (data.getType() == OMMTypes.ENUM) {
						} else {
							parseData( data );
						}

						if ("VALUE_DT1".equals( fiddef.getName() )) {
							Locale locale = Locale.US;
							SimpleDateFormat df = new SimpleDateFormat( "dd MMM yyyy", locale );
							String dateString = null;

							try {
								Date date = df.parse( data.toString() );
								SimpleDateFormat sdf = new SimpleDateFormat( "yyyy/MM/dd" );
								dateString = sdf.format( date );
								logger.debug( "Identifier : {}, field : DATE = {}", new Object[] { itemName, dateString } );

								// QuoteStream.quoteMap.put(itemName+",DATE", dateString);
							} catch (ParseException e) {
							}

						} else if ("VALUE_TS1".equals( fiddef.getName() )) {
							logger.debug( "Identifier : {}, field : TIME = {}", new Object[] { itemName, data.toString() } );

							// QuoteStream.quoteMap.put( itemName + ",TIME", data.toString() );
						} else {
							if (fields.contains( fiddef.getName() )) {
								logger.debug( "Identifier : {}, field : {} = {}", new Object[] { itemName, fiddef.getName(), data.toString() } );

								Cache cache = cacheManager.getCache( "indexes" );
								cache.put( itemName + fiddef.getName(), data.toString() );
							}
						}
					} else {
					}
				} else {
					// dumpFieldEntryHeader(fe, null);
					if (fe.getDataType() == OMMTypes.UNKNOWN) {
						// OMMDataBuffer data = (OMMDataBuffer)fe.getData();
						// System.out.println(HexDump.toHexString(data.getBytes(), false));
					} else { // defined data already has type
						OMMData data = fe.getData();
						parseData( data );
					}
				}
				break;
			case OMMTypes.ELEMENT_ENTRY:
				// dumpElementEntryHeader((OMMElementEntry)entry);
				parseData( entry.getData() );

				break;
			case OMMTypes.MAP_ENTRY:
				// dumpMapEntryHeader((OMMMapEntry)entry);
				if ((((OMMMapEntry) entry).getAction() != OMMMapEntry.Action.DELETE) && entry.getDataType() != OMMTypes.NO_DATA) {
					parseData( entry.getData() );
				}

				break;
			case OMMTypes.VECTOR_ENTRY:
				// dumpVectorEntryHeader((OMMVectorEntry)entry);
				if ((((OMMVectorEntry) entry).getAction() != OMMVectorEntry.Action.DELETE)
				        && (((OMMVectorEntry) entry).getAction() != OMMVectorEntry.Action.CLEAR)) {
					parseData( entry.getData() );
				}

				break;
			case OMMTypes.FILTER_ENTRY:
				// dumpFilterEntryHeader((OMMFilterEntry)entry);

				if (((OMMFilterEntry) entry).getAction() != OMMFilterEntry.Action.CLEAR) {
					parseData( entry.getData() );
				}

				break;
			default:
				// dumpEntryHeader(entry);
				parseData( entry.getData() );
				break;

			}
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}
	}

	public Collection<String> getFields() {
		return fields;
	}

	public void setFields(Collection<String> fields) {
		this.fields = fields;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
}
