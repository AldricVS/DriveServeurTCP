package test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import process.database.DatabaseManager;

/**
 * Unit tests in order to check if server can contact the database seen in course.
 * Tests where made on database created with the TD 1 SQL file from database course
 * @author Aldric Vitali Silvestre
 */
public class TestEtuDatabase {
	DatabaseManager databaseManager;
	
	@Before
	public void connectToDatabase() throws SQLException {
		databaseManager = new DatabaseManager("localhost:5432/dbetu", "etu", "A123456");
	}
	
	@Test
	public void getSpecificCentralUnit() throws SQLException{
		String query = "SELECT * FROM uc WHERE no_uc = 'UnitC070009'";
		ResultSet resultSet = databaseManager.excecuteSingleQuery(query);
		resultSet.next();
		
		String unitMemory = resultSet.getString("memoire");
		//if unitMemory is null, it means that row doesn't have this column name (not normal)
		assertNotNull(unitMemory);
		//obviously, unityMemory have a predictable value
		assertEquals("4", unitMemory);
	}
	
	@Test 
	public void checkBreakdowns() throws SQLException{
		//we have two breakdowns in the database, so we need to count... two of them in the result
		String query = "SELECT obj_invent FROM panne";
		ResultSet resultSet = databaseManager.excecuteSingleQuery(query);
		resultSet.last();
		assertEquals(2, resultSet.getRow());
		assertEquals("UnitC070010", resultSet.getString("obj_invent"));
	}
}
