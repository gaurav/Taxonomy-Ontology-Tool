package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaleoDBBulkMerger {

	
	private PaleoDBBulkMerger testMerger;
	
	private final File testTaxonomic_units1 = new File("src/SampleProcessFiles/Tyrannosaurus/taxonomic_units.dat");

	@Before
	public void setUp() throws Exception {
		testMerger = new PaleoDBBulkMerger();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMerge() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetColumns() {
		fail("Not yet implemented");
	}

}
