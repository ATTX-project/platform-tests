package org.uh.attx.platform.test;

import org.junit.Before;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class IndexingServiceIntegrationTest {
    public final String API_VERSION = "0.2";

    @Before
    public void checkHealth(){
        TestUtils.getIndexingService();
    }
}
