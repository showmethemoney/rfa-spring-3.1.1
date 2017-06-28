package test.com.metrics.reuters;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.prefs.Preferences;

import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHealthCheck
{
	protected static final Logger logger = LoggerFactory.getLogger( TestHealthCheck.class );

	@Test
	public void testCheck() {
		try {
			String feedConfigFilename = "C:/Users/ethan/workspace/rfa-spring-3.1.1/src/test/resources/FeedConfig.xml";

			Preferences.importPreferences( new DataInputStream( new FileInputStream( feedConfigFilename ) ) );

			Preferences preferences = Preferences.userRoot();

			logger.info( preferences.node( "com/reuters/rfa/myNS/Connections/CONNECT_RSSED" ).get( "connectionType", "" ) );
			logger.info( preferences.node( "com/reuters/rfa/myNS/Connections/CONNECT_RSSED" ).get( "portNumber", "" ) );
			logger.info( preferences.node( "com/reuters/rfa/myNS/Connections/CONNECT_RSSED" ).get( "serverList", "" ) );

			TelnetClient client = new TelnetClient();
			client.setConnectTimeout( 1000 );
			client.connect( "www.globalfundflow.com", 14002 );
			logger.info( "{}", client.isConnected() );
			
			client.disconnect();
		} catch (Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}
	}
}
