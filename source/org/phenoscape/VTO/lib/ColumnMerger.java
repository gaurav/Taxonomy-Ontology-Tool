package org.phenoscape.VTO.lib;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.Synonym;

public class ColumnMerger implements Merger,ColumnFormat {
	
	private final String columnSeparator; 
	private final ColumnReader reader;
	
	private Map<Integer,String> synPrefixMap;
	
	private Map<KnownField,Integer> columnNums = new HashMap<KnownField,Integer>();
	
	static Logger logger = Logger.getLogger(ColumnMerger.class.getName());

	public ColumnMerger(String separator){
		columnSeparator = separator;
		reader = new ColumnReader(columnSeparator);
	}
	
	
	@Override
	public void setColumns(List<String> columns, Map<Integer,String> synPrefixes) {
		reader.setColumns(columns,synPrefixes);  // and what else?
		synPrefixMap = synPrefixes;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		ItemList items = reader.processCatalog(source, true);
		for(Item item : items.getContents()){
			
		}
		// TODO finish

	}

	@Override
	public boolean canAttach() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void attach(File source, TaxonStore target, String attachment, String cladeRoot, String prefix) {
		ItemList items = reader.processCatalog(source, true);
		final Map<String,IdentifiedObject> classIDs = new HashMap<String,IdentifiedObject>();
		IdentifiedObject rootClass = null;
		Term parentTerm = null;
		if (!"".equals(attachment)){
			parentTerm = target.getTermbyName(attachment);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + source.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
		if (items.hasColumn(KnownField.CLASS)){
			for (Item it : items.getContents()){
				final String className = it.getName(KnownField.CLASS);
				Term classTerm = target.getTermbyName(className);
				if (classTerm == null){
					classTerm = target.addTerm(className);
					target.setRankFromName(classTerm,KnownField.CLASS.getCannonicalName());   //string from knownColumn?
					if (parentTerm != null)  // this is weak, but allows construction of an ontology with multiple roots (so obviously wrong)
						target.attachParent(classTerm, parentTerm);
				}
			}
		}
		if (items.hasColumn(KnownField.ORDER)){
			for (Item it : items.getContents()){
				final String orderName = it.getName(KnownField.ORDER);
				Term orderTerm = target.getTermbyName(orderName);
				if (orderTerm == null){
					orderTerm = target.addTerm(orderName);
					target.setRankFromName(orderTerm,KnownField.ORDER.getCannonicalName());
					if (it.hasColumn(KnownField.CLASS) && target.getTermbyName(it.getName(KnownField.CLASS)) != null){
						final String parentName = it.getName(KnownField.CLASS);
						target.attachParent(orderTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(orderTerm, parentTerm);
				}
			}
		}
		if (items.hasColumn(KnownField.FAMILY)){
			for (Item it : items.getContents()){
				final String familyName = it.getName(KnownField.FAMILY);
				Term familyTerm = target.getTermbyName(familyName);
				if (familyTerm == null){
					familyTerm = target.addTerm(familyName);
					target.setRankFromName(familyTerm,KnownField.FAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(it.getName(KnownField.ORDER)) != null){
						final String parentName = it.getName(KnownField.ORDER);
						target.attachParent(familyTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(familyTerm, parentTerm);
				}
			}
		}
		if (items.hasColumn(KnownField.SUBFAMILY)){
			for (Item it : items.getContents()){
				final String subFamilyName = it.getName(KnownField.SUBFAMILY);
				Term subFamilyTerm = target.getTermbyName(subFamilyName);
				if (subFamilyTerm == null){
					subFamilyTerm = target.addTerm(subFamilyName);
					target.setRankFromName(subFamilyTerm, KnownField.SUBFAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(subFamilyTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(subFamilyTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.GENUS)){
			for (Item it : items.getContents()){
				final String genusName = it.getName(KnownField.GENUS);
				Term genusTerm = target.getTermbyName(genusName);
				if (genusTerm == null){
					genusTerm = target.addTerm(genusName);
					target.setRankFromName(genusTerm, KnownField.GENUS.getCannonicalName());
					if (it.hasColumn(KnownField.SUBFAMILY) && target.getTermbyName(it.getName(KnownField.SUBFAMILY)) != null){
						final String parentName = it.getName(KnownField.SUBFAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));						
					}
					else if (parentTerm != null)
						target.attachParent(genusTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.CLADE)){   // This was used in amphibianet
			for (Item it : items.getContents()){
				String cladeLevelName = it.getName(KnownField.CLADE);  //call it a cladeLevel to reduce ambiguity
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					final String parentName = it.getName(KnownField.GENUS);   //if it has a known parent (it ought to) render the parent genus parenthetically
					cladeLevelName = cladeLevelName + " (" + parentName + ")";
				}
				Term cladeLevelTerm = target.getTermbyName(cladeLevelName);
				if (cladeLevelTerm == null){
					cladeLevelTerm = target.addTerm(cladeLevelName);
					target.setRankFromName(cladeLevelTerm, KnownField.CLADE.getCannonicalName());
					if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
						final String parentName = it.getName(KnownField.GENUS);
						target.attachParent(cladeLevelTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(cladeLevelTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.SPECIES)){
			for (Item it : items.getContents()){
				final String speciesName = it.getName(KnownField.GENUS) + " " + it.getName(KnownField.SPECIES);
				Term speciesTerm = target.getTermbyName(speciesName); 
				if (speciesTerm == null){
					speciesTerm = target.addTerm(speciesName);
					target.setRankFromName(speciesTerm,KnownField.SPECIES.getCannonicalName());
				}
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					final String parentName = it.getName(KnownField.GENUS);
					target.attachParent(speciesTerm,target.getTermbyName(parentName));
				}
				else if (it.hasColumn(KnownField.CLADE) && target.getTermbyName(it.getName(KnownField.CLADE)) != null){
					final String parentName = it.getName(KnownField.CLADE);
					target.attachParent(speciesTerm,target.getTermbyName(parentName));
				}
				else if (parentTerm != null)
					target.attachParent(speciesTerm, parentTerm);
				if (items.hasColumn(KnownField.XREF)){
					
				}
				Collection<String> synSources = it.getSynonymSources();
				for (String synSource : synSources){
					for(String syn : it.getSynonymsForSource(synSource))
						if (true) { //!syn.equals(speciesName)){
							String[] sourceComps = synSource.split(":",2);
							SynonymI s = target.makeSynonymWithXref(syn, sourceComps[0], sourceComps[1]);
							speciesTerm.addSynonym(s);
						}
				}		
			}
		}
	}

	


}