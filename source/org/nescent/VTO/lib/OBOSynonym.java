package org.nescent.VTO.lib;

import org.obo.datamodel.Synonym;

public class OBOSynonym implements SynonymI {
	
	private Synonym syn;
	

	public OBOSynonym(Synonym s) {
		syn = s;
	}
	
	public Synonym asOBOSynonym(){
		return syn;
	}

	@Override
	public String getID() {
		return syn.getID();
	}
	
	public String getText(){
		return syn.getText();
	}
	
	
}
