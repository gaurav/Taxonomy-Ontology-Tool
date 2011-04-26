/**
 * VTOTool - a tool for merging and building ontologies from multiple taxonomic sources
 * 
 * Peter Midford
 * 
 */
package org.phenoscape.VTO;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.phenoscape.VTO.lib.CoLMerger;
import org.phenoscape.VTO.lib.IOCMerger;
import org.phenoscape.VTO.lib.ITISMerger;
import org.phenoscape.VTO.lib.Merger;
import org.phenoscape.VTO.lib.NCBIMerger;
import org.phenoscape.VTO.lib.OBOMerger;
import org.phenoscape.VTO.lib.OBOStore;
import org.phenoscape.VTO.lib.OWLMerger;
import org.phenoscape.VTO.lib.ColumnMerger;
import org.phenoscape.VTO.lib.OWLStore;
import org.phenoscape.VTO.lib.TaxonStore;
import org.phenoscape.VTO.lib.UnderscoreJoinedNamesMerger;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Builder {
	
	final static String OBOFORMATSTR = "OBO";
	final static String ITISFORMATSTR = "ITIS";
	final static String NCBIFORMATSTR = "NCBI";
	final static String CSVFORMATSTR = "CSV";
	final static String TSVFORMATSTR = "TSV";
	final static String OWLFORMATSTR = "OWL";
	final static String IOCFORMATSTR = "IOC";
	final static String COLFORMATSTR = "COL";
	final static String JOINEDNAMETABBEDCOLUMNS = "JOINEDNAMETAB";
	final static String XREFFORMATSTR = "XREF";    //This isn't a store format, but is a target
	final static String COLUMNFORMATSTR = "COLUMN";  //This isn't (necessary) a store format, but is a target
	final static String SYNONYMFORMATSTR = "SYNONYM"; //This a variant of the column format
	
	final static String ATTACHACTIONSTR = "attach";
	final static String MERGEACTIONSTR = "merge";
	final static String TRIMACTIONSTR = "trim";
	
	final static String COLUMNSYNTAXSTR = "column";
	
	
	final static String PREFIXITEMSTR = "prefix";
	final static String FILTERPREFIXITEMSTR = "filterprefix";
	
	final private File optionsFile;
	
	
	static final Logger logger = Logger.getLogger(Builder.class.getName());



	Builder(File options){
		optionsFile = options;
	}

	public void build() throws IOException {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		NodeList nl = null;
		try {
			DocumentBuilder db = f.newDocumentBuilder();
			db.setErrorHandler(new DefaultHandler());
			Document d = db.parse(optionsFile);
			nl = d.getElementsByTagNameNS("","taxonomy");
		} catch (ParserConfigurationException e) {
			logger.fatal("Error in initializing parser");
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			logger.fatal("Error in parsing " + optionsFile.getCanonicalPath());
			logger.fatal("Exception message is: " + e.getLocalizedMessage());
			return;
		} 
		if (nl.getLength() != 1){
			logger.fatal("Error - More than one taxonomy element in " + optionsFile.getCanonicalPath());
			return;
		}
		Node taxonomyRoot = nl.item(0);
		String targetURLStr = getAttribute(taxonomyRoot,"target");
		String targetFormatStr = getAttribute(taxonomyRoot,"format");
		String targetRootStr = getAttribute(taxonomyRoot,"root");
		String targetPrefixStr = getAttribute(taxonomyRoot,PREFIXITEMSTR);
		String targetFilterPrefixStr = getAttribute(taxonomyRoot,FILTERPREFIXITEMSTR);
		TaxonStore target = getStore(targetURLStr, targetPrefixStr, targetFormatStr);
		logger.info("Building taxonomy to save at " + targetURLStr + " in the " + targetFormatStr + " format\n");
		NodeList actions = taxonomyRoot.getChildNodes();
		for(int i=0;i<actions.getLength();i++){
			Node action = actions.item(i);
			String actionName = action.getNodeName();
			@SuppressWarnings("unchecked")
			List<String> columns = (List<String>)Collections.EMPTY_LIST;
			Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
			if (ATTACHACTIONSTR.equalsIgnoreCase(actionName)){
				processAttachAction(action,target, targetRootStr, targetPrefixStr);
			}
			else if (MERGEACTIONSTR.equalsIgnoreCase(actionName)){
				NodeList childNodes = action.getChildNodes();
				if (childNodes.getLength()>0){
					columns = processChildNodesOfAttach(childNodes,synPrefixes);
				}
				String formatStr = getAttribute(action,"format");
				Merger m = getMerger(formatStr,columns,synPrefixes);
				String sourceURLStr = getAttribute(action,"source");
				String mergePrefix = getAttribute(action,PREFIXITEMSTR);
				File sourceFile = null;  //CoL doesn't specify a fixed URL, we're not loading from one source - maybe this is too much of a special case
				if (!"".equals(sourceURLStr))
					sourceFile = getSourceFile(sourceURLStr);
				logger.info("Merging names from " + sourceURLStr);
				if (mergePrefix == null){
					m.merge(sourceFile, target, targetPrefixStr);
				}
				else {
					m.merge(sourceFile, target, mergePrefix);
				}
			}
			else if (TRIMACTIONSTR.equalsIgnoreCase(actionName)){
				String nodeStr = getAttribute(action,"node");
				target.trim(nodeStr);
			}
			else if (action.getNodeType() == Node.TEXT_NODE){
				//ignore
			}
			else if (action.getNodeType() == Node.COMMENT_NODE){
				//ignore
			}
			else{
				logger.warn("Unknown action: " + action);
			}
		}
		if ("XREF".equals(targetFormatStr)){
			target.saveXref(targetFilterPrefixStr);
		}
		else if ("COLUMN".equals(targetFormatStr)){
			target.saveColumnsFormat(targetFilterPrefixStr);
		}
		else if ("SYNONYM".equals(targetFormatStr)){
			target.saveSynonymFormat(targetFilterPrefixStr);
		}
		else{
			target.saveStore();
		}
	}

	private void processAttachAction(Node action, TaxonStore target, String targetRootStr, String targetPrefixStr){
		@SuppressWarnings("unchecked")
		List<String> columns = (List<String>)Collections.EMPTY_LIST;
		Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
		String formatStr = getAttribute(action,"format");
		String cladeRootStr = getAttribute(action,"root");
		String sourceParentStr = getAttribute(action,"parent");
		NodeList childNodes = action.getChildNodes();
		if (childNodes.getLength()>0){
			columns = processChildNodesOfAttach(childNodes,synPrefixes);
		}
		Merger m = getMerger(formatStr,columns,synPrefixes);
		if (!m.canAttach()){
			logger.error("Error - Merger for format " + formatStr + " can't attach branches to the tree");
		}
		String sourceURLStr = action.getAttributes().getNamedItem("source").getNodeValue();
		File sourceFile = getSourceFile(sourceURLStr);
		logger.info("Attaching taxonomy from " + sourceURLStr);
		if (targetPrefixStr == null){
			logger.warn("No prefix for newly generated ids specified - will default to filename component");
			targetPrefixStr = sourceFile.getName();
		}
		if (sourceParentStr != null){   //need to specify the clade within the sourceFile (or null?)
			if (cladeRootStr != null)
				m.attach(sourceFile,target,sourceParentStr,cladeRootStr,targetPrefixStr);
			else
				m.attach(sourceFile,target,sourceParentStr,sourceParentStr,targetPrefixStr);
		}
		else {
			if (cladeRootStr != null)
				m.attach(sourceFile,target,targetRootStr,cladeRootStr,targetPrefixStr);
			else
				m.attach(sourceFile,target,targetRootStr,targetRootStr,targetPrefixStr);
		}

	}
	
	
	
	
	private List<String> processChildNodesOfAttach(NodeList childNodes, Map<Integer, String> synPrefixes) {
		List<String> result = new ArrayList<String>();
		for(int i = 0; i<childNodes.getLength();i++){
			Node child = childNodes.item(i);
			String childName = child.getNodeName();
			if ("columns".equals(childName)){
				NodeList columnNames = child.getChildNodes();
				for(int j = 0; j<columnNames.getLength();j++){
					Node column = columnNames.item(j);
					if (column.getNodeType() == Node.ELEMENT_NODE){
						result.add(column.getNodeName());
						if (column.getAttributes().getLength()>0){
							String synPrefix = column.getAttributes().getNamedItem(PREFIXITEMSTR).getNodeValue();
							synPrefixes.put(j, synPrefix);
						}
					}
				}
				
			}
			else if (child.getNodeType() == Node.TEXT_NODE){
				//ignore
			}
			else{
				logger.warn("Unknown subelement" + child);
			}
		}
		return result;
	}

	private TaxonStore getStore(String targetURLStr, String prefixStr, String formatStr) {
		if (OBOFORMATSTR.equals(formatStr)){
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					logger.error("OBO format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				return new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		if (OWLFORMATSTR.equals(formatStr)){
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					logger.error("OWL format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				return new OWLStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (XREFFORMATSTR.equals(formatStr)){      //XREF isn't a storage format, so the store is implementation dependent (currently OBO)
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					logger.error("XREF format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				OBOStore result = new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
				return result;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		if (COLUMNFORMATSTR.equals(formatStr)){      //COLUMN isn't a storage format, so the store is implementation dependent (currently OBO)
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					logger.error("Column format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				OBOStore result = new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
				return result;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		if (SYNONYMFORMATSTR.equals(formatStr)){      //COLUMN isn't a storage format, so the store is implementation dependent (currently OBO)
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					logger.error("Column format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				OBOStore result = new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
				return result;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		logger.error("Format " + formatStr + " not supported for merging");
		return null;
	}
	
	
	private Merger getMerger(String formatStr, List<String> columns, Map<Integer, String> synPrefixes){
		if (OBOFORMATSTR.equals(formatStr))
			return new OBOMerger();
		if (ITISFORMATSTR.equals(formatStr))
			return new ITISMerger();
		if (NCBIFORMATSTR.equals(formatStr))
			return new NCBIMerger();
		if (CSVFORMATSTR.equals(formatStr)){
			ColumnMerger result = new ColumnMerger(",");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (TSVFORMATSTR.equals(formatStr)){
			ColumnMerger result = new ColumnMerger("\t");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (JOINEDNAMETABBEDCOLUMNS.equals(formatStr)){
			UnderscoreJoinedNamesMerger result = new UnderscoreJoinedNamesMerger("\t");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (OWLFORMATSTR.equals(formatStr)){
			return new OWLMerger();
		}
		if (IOCFORMATSTR.equals(formatStr)){
			return new IOCMerger();
		}
		if (COLFORMATSTR.equals(formatStr)){
			return new CoLMerger();
		}
		logger.error("Format " + formatStr + " not supported for merging");
		return null;
	}
	
	private File getSourceFile(String sourceURLStr) {
		try {
			URL u = new URL(sourceURLStr);
			if (!"file".equals(u.getProtocol())){
				System.err.println("Can only load from a local file");
				return null;
			}
			return new File(u.getFile());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

// Utilities
	String getAttribute(Node n,String attribute_id){
		final Node attNode = n.getAttributes().getNamedItem(attribute_id);
		if (attNode != null)
			return attNode.getNodeValue();
		else
			return null;
	}
	
	


}



