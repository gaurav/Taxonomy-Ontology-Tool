package org.nescent.VTO.lib;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

class PBDBItem{
	
	static final Pattern pipePattern = Pattern.compile("\\|");
	
	//columns for itis format
	static final int IDCOLUMN = 0;
	static final int NAMECOLUMN = 2;
	static final int STATUSCOLUMN = 3;
	static final int STATUSDETAILCOLUMN = 4;
	static final int ENTRYDATECOLUMN = 9;
	static final int PARENTNAMECOLUMN = 10;
	static final int KINGDOMNAMECOLUMN = 13;
	static final int RANKNAMECOLUMN = 14;
	static final int MODDATECOLUMN = 15;
	
	static final Pattern commaPattern = Pattern.compile("\\,");
	
	
	private String name;
	final private int id;
	final private TaxonomicStatus status;
	final private String parentName;
	final private String rankName;
	
	
	static final Logger logger = Logger.getLogger(PBDBItem.class.getName());
	
	enum TaxonomicStatus{
		VALID,
		JUNIOR_SYNONYM,
		NOMEN_NUDUM,
		NOMEN_DUBIUM,
		ORIGINALNAME_COMBINATION,
		MISPLACED,
		INVALID_SUBGROUP,
		INCORRECT_ORIGINAL_SPELLING,
		UNRECOGNIZED
	}

			
	PBDBItem(String line){
		final String[] digest = pipePattern.split(line);
		if (digest.length != 16){
			throw new RuntimeException("Line had wrong number of elements: " + digest.length);
		}
		try {
			id =Integer.parseInt(digest[IDCOLUMN]);
		}
		catch (NumberFormatException e){
			throw new RuntimeException("Misformatted taxon ID in line " + line);
		}
		if (digest[NAMECOLUMN].isEmpty()){
			throw new RuntimeException("Misformatted taxon name in line " + line);
		} 
		String taxonName = digest[NAMECOLUMN];
		if(taxonName.charAt(0) == '"'){
			if (taxonName.charAt(taxonName.length()-1) == '"'){
				taxonName = taxonName.substring(1, taxonName.length()-1);
			}
			else
				throw new RuntimeException("Misformatted taxon name in line " + line);
		}
		this.name=taxonName;
		if (digest[PARENTNAMECOLUMN].isEmpty()){
			throw new RuntimeException("Misformatted parent name in line " + line);
		}
		parentName = digest[PARENTNAMECOLUMN];
		
		if (digest[RANKNAMECOLUMN].isEmpty()){
			throw new RuntimeException("Misformatted rank name in line " + line);
		}
		rankName = digest[RANKNAMECOLUMN];
		//Status processing - might be extended...
		if ("valid".equalsIgnoreCase(digest[STATUSCOLUMN]))
			status = TaxonomicStatus.VALID;
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && "\"junior synonym\"".equalsIgnoreCase(digest[STATUSDETAILCOLUMN])){
			status = TaxonomicStatus.JUNIOR_SYNONYM;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				 digest[STATUSDETAILCOLUMN] != null && 
				 digest[STATUSDETAILCOLUMN].startsWith("\"nomen nudum")){
			status = TaxonomicStatus.NOMEN_NUDUM;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				 digest[STATUSDETAILCOLUMN] != null && 
				 digest[STATUSDETAILCOLUMN].startsWith("\"nomen dubium")){
			status = TaxonomicStatus.NOMEN_DUBIUM;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				 "\"original name/combination\"".equals(digest[STATUSDETAILCOLUMN])){
			status = TaxonomicStatus.ORIGINALNAME_COMBINATION;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				digest[STATUSDETAILCOLUMN].startsWith("\"belongs to")){
			status = TaxonomicStatus.MISPLACED;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				"\"invalid subgroup\"".equals(digest[STATUSDETAILCOLUMN])){
			status = TaxonomicStatus.INVALID_SUBGROUP;
		}
		else if ("invalid".equalsIgnoreCase(digest[STATUSCOLUMN]) && 
				"\"unavailable, incorrect original spelling\"".equals(digest[STATUSDETAILCOLUMN])){
			status = TaxonomicStatus.INCORRECT_ORIGINAL_SPELLING;
		}
		else
			status = TaxonomicStatus.UNRECOGNIZED;
		if (this.status == TaxonomicStatus.UNRECOGNIZED){
			//logger.error("Unrecognized taxonomic status " + digest[STATUSCOLUMN] + ") or status detail (" + digest[STATUSDETAILCOLUMN] + ") in line " + line);
		}

	}
	
	String getName(){
		return name;
	}
	
	int getId(){
		return id;
	}
	
	String getParentName(){
		return parentName;
	}
	
	boolean isValid(){
		return (status == TaxonomicStatus.VALID);
	}
	
}