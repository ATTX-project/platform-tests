package org.uh.attx.platform.test;

import org.junit.Before;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * Created by stefanne on 11/16/17.
 */

@RunWith(JUnitPlatform.class)
public class ProvServiceIntegrationTest {

    public final String API_VERSION = "0.2";

    @Before
    public void checkHealth(){
        TestUtils.testProvServiceHealth();
    }
}
