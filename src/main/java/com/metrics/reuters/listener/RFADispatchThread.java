package com.metrics.reuters.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reuters.rfa.common.EventQueue;

public class RFADispatchThread extends Thread
{
	protected static final Logger logger = LoggerFactory.getLogger( RFADispatchThread.class );
	private EventQueue eventQueue = null;
	private long timeToDispatch = 0l;
	
	public RFADispatchThread() {
	}

	public RFADispatchThread(EventQueue eventQueue, long timeToDispatch) {
		this.eventQueue = eventQueue;
		this.timeToDispatch = timeToDispatch;
	}

    @Override
	public void run() {
		while (true) {
			try {
				eventQueue.dispatch( timeToDispatch ); 
			} catch (Throwable cause) {
				logger.error( cause.getMessage(), cause );

				throw new RuntimeException( cause );
			}  
		}
	}
}
