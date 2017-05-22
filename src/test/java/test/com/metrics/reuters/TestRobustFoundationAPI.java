package test.com.metrics.reuters;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.metrics.reuters.client.ItemManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-rfa.xml" })
public class TestRobustFoundationAPI
{
	protected static final Logger logger = LoggerFactory.getLogger( TestRobustFoundationAPI.class );
	@Autowired
	private ItemManager itemManager = null;
	
	@Test
	public void testRun() {
		try {
			logger.info( "hello!!" );
			Assert.assertNotNull( itemManager );
			logger.info( "hello!!" );
		} catch(Throwable cause) {
			logger.error( cause.getMessage(), cause );
		}
	}
}
