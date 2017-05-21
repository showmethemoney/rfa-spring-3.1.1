package com.metrics.reuters.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.metrics.reuters.RobustFoundationAPI;

public class RFADispatchListener implements ApplicationListener<ContextRefreshedEvent>
{
	protected static final Logger logger = LoggerFactory.getLogger( RFADispatchListener.class );

	private RobustFoundationAPI robustFoundationAPI = null;
	private long timeToDispatch = 0l;
	
	public void onApplicationEvent(ContextRefreshedEvent event) {

		if (null != robustFoundationAPI) {
			new RFADispatchThread( robustFoundationAPI.getEventQueue(), 0l == getTimeToDispatch() ? 1000 : getTimeToDispatch() ).run();
		}
	}

	public RobustFoundationAPI getRobustFoundationAPI() {
		return robustFoundationAPI;
	}

	public void setRobustFoundationAPI(RobustFoundationAPI robustFoundationAPI) {
		this.robustFoundationAPI = robustFoundationAPI;
	}

	public long getTimeToDispatch() {
		return timeToDispatch;
	}

	public void setTimeToDispatch(long timeToDispatch) {
		this.timeToDispatch = timeToDispatch;
	}
	
}
