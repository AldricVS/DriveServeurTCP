package test.unit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Global test suite for unit tests.
 * 
 * @author Aldric Vitali Silvestre
 */
@RunWith(Suite.class)
@SuiteClasses({
	TestEtuDatabase.class
})
public class MainTestSuite {
}
