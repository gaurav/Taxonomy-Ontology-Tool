/*
 * VTOTool - a utility build taxonomy ontologies from multiple sources 
 * 
 * Copyright (c) 2007-2011 Peter E. Midford
 *
 * Licensed under the 'MIT' license (http://opensource.org/licenses/mit-license.php)
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Created June 2010, based on ItemReader from OBOVocab
 * Last updated on August 22, 2011
 *
 */
package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 
 * @author pmidford
 *
 */
public class ColumnReader {


	static final String[] IGNORELIST = {};

	static final String DESCRIPTIONSTR = "Description";
	static final String STATUSSTR = "Status";

	final Pattern splitPattern;


	private List<Integer> synonymFields = new ArrayList<Integer>();
	private List<ColumnType> headers;

	static final Logger logger = Logger.getLogger(ColumnReader.class.getName());


	/**
	 * Constructor just sets the column delimiting character (generally tab or comma) as a string.
	 * @param splitString
	 */
	public ColumnReader(String splitString){
		splitPattern = Pattern.compile(splitString);
	}

	/**
	 * This method attaches known tags to the columns (or ignore) as specified in columns element in taxonOptions.xml.
	 * Besides tags, this method provides a way to specify the source of synonyms (e.g., from another known database).
	 * This allows column configuration without guessing from labels appearing in column headers
	 * @param columns
	 * @param synonymRefs
	 */
	public void setColumns(final List<ColumnType> columns){
		headers = columns;
	}


	/**
	 * 
	 * @param f
	 * @param headersFirst
	 * @return list of items parsed from the spreadsheet file
	 */
	public ItemList processCatalog(File f,boolean headersFirst) {
		final ItemList result = new ItemList();
		result.addColumns(headers);
		String raw = "";
		if (f != null){
			try {
				final BufferedReader br = new BufferedReader(new FileReader(f));
				if (headersFirst){  //ignore headers, fields are defined in the xml configuration
					raw=br.readLine();
				}
				raw = br.readLine();
				while (raw != null){
					final String[] digest = splitPattern.split(raw);
					if (checkEntry(digest)){
						Item foo = processLine(digest,result);
						result.addItem(foo);
					}
					else{
						System.err.println("Bad line: " + raw);
					}
					raw = br.readLine();
				}
			}
			catch (IOException e) {
				System.out.print(e);
				return result;
			}
		}
		return result; // for now
	}

	// what checks are needed?
	private boolean checkEntry(String[] line){
		if (line.length < 3)
			return false;
		return true;
	}



	private Item processLine(String[] digest, ItemList resultList){
		final Item result = new Item(); 
		for(int i = 0;i<headers.size();i++){   //this allows ignoring trailing fields that are undefined in the xml columns element
			if (digest.length>i){ //this allows files that are (unfortunately) missing trailing empty fields (Excel can write tab files like this)
				String rawColumn = digest[i]; 
				if (rawColumn.length() > 2 && rawColumn.charAt(0) == '"' && rawColumn.charAt(rawColumn.length()-1) == '"')
					rawColumn = rawColumn.substring(1,rawColumn.length()-1);
				final String curColumn = rawColumn.trim();  //At least some sources have extra trailing white space in names 
				KnownField f = headers.get(i).getFieldType();
				switch(f){
				case SYNONYM: {
					if (!rawColumn.isEmpty()){
						if (headers.get(i).getXrefTemplate() == null || headers.get(i).getXrefTemplate().isEmpty()){
							String syns[] = rawColumn.split(",");
							for(String syn : syns){
								String trimmedSyn = syn.trim();
								result.addPlainSynonym(trimmedSyn);
							}
						}
						else {
							String template = headers.get(i).getXrefTemplate();
							while(template.indexOf("*xref") != -1){
								final int p = template.indexOf("*xref");
								template = template.substring(0,p) + rawColumn.trim() + template.substring(p+5);
							}
							String syns[] = rawColumn.split(",");
							for(String syn : syns){
								String trimmedSyn = syn.trim();
								result.addSynonymWithXref(trimmedSyn,template);
							}
						}
					}

					break;
				}
				case VERNACULAR: {
					if (!rawColumn.isEmpty()){
						String syns[] = rawColumn.split(",");
						for(String syn : syns){
							String trimmedSyn = syn.trim();
							result.addVernacular(trimmedSyn);
						}
					}
					break;
				}
				case COMMENT: {
					if (!rawColumn.isEmpty()){
						result.setComment(rawColumn.trim());
					}
					break;
				}
				case STATUS: {
					break;
				}
				case XREF: {
					if (!rawColumn.isEmpty()){
						String template = headers.get(i).getXrefTemplate();
						if (template == null){
							template = "*xref";
						}
						while(template.indexOf("*xref") != -1){
							final int p = template.indexOf("*xref");
							template = template.substring(0,p) + rawColumn.trim() + template.substring(p+5);
						}
						result.addXref(template);
					}
					break;
				}
				case DELIMITEDNAME: {
					
					break;
				}
				default: {
					if (f.isTaxon()){
						result.putName(f,curColumn);
					}
				}

				}
			}
		}
		return result;
	}

	public ColumnType getColumn(KnownField field) {
		for (ColumnType c : headers){
			if (field.getCannonicalName().equals(c.getType()))
				return c;
		}
		return null;
	}        

}
