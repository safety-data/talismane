///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.lexicon;

import gnu.trove.map.hash.THashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A default lexical entry, which can be overridden to provide specific morphology rules (especially getMorphologyForConll()).
 * @author Assaf Urieli
 *
 */
class CompactLexicalEntry extends AbstractLexicalEntry implements WritableLexicalEntry, Serializable {
	private static final long serialVersionUID = 5L;
	
	private LexiconFile lexiconFile;
	private String word = "";
	private String lemma = "";
	private int attributeMarker = 0x0;
	private byte[] attributeCodes = new byte[0];
	
	private static final Map<LexicalAttribute,Integer> attributeBitMap = new THashMap<LexicalAttribute, Integer>();
	
	static {
		attributeBitMap.put(LexicalAttribute.Aspect, 0x1);
		attributeBitMap.put(LexicalAttribute.Case, 0x2);
		attributeBitMap.put(LexicalAttribute.Category, 0x4);
		attributeBitMap.put(LexicalAttribute.Gender, 0x8);
		attributeBitMap.put(LexicalAttribute.LemmaComplement, 0x10);
		attributeBitMap.put(LexicalAttribute.Mood, 0x20);
		attributeBitMap.put(LexicalAttribute.Morphology, 0x40);
		attributeBitMap.put(LexicalAttribute.Number, 0x80);
		attributeBitMap.put(LexicalAttribute.Person, 0x100);
		attributeBitMap.put(LexicalAttribute.PossessorNumber, 0x200);
		attributeBitMap.put(LexicalAttribute.SubCategory, 0x400);
		attributeBitMap.put(LexicalAttribute.Tense, 0x800);

		attributeBitMap.put(LexicalAttribute.OtherAttribute1, 0x10000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute2, 0x20000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute3, 0x40000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute4, 0x80000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute5, 0x100000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute6, 0x200000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute7, 0x400000);
		attributeBitMap.put(LexicalAttribute.OtherAttribute8, 0x800000);
	}
	
	public CompactLexicalEntry(LexiconFile lexiconFile) {
		super();
		this.lexiconFile = lexiconFile;
	}
	
	public boolean hasAttribute(LexicalAttribute attribute) {
		if (attribute==LexicalAttribute.Word||attribute==LexicalAttribute.Lemma)
			return true;
		int attributeBit = attributeBitMap.get(attribute);
		return ((attributeMarker & attributeBit)>0);
	}
	
	private String getValue(LexicalAttribute attribute) {
		int attributeBit = attributeBitMap.get(attribute);
		if ((attributeMarker & attributeBit)==0) {
			return "";
		}
		// count bits to the left of current marker
		int bitCount = 0;
		for (int i=0x1; i<attributeBit; i*=2) {
			if ((attributeMarker & i)>0)
				bitCount++;
		}
		byte code = attributeCodes[bitCount];
		String value = lexiconFile.getAttributeValue(attribute, code);
		return value;
	}
	
	private void setValue(LexicalAttribute attribute, String value) {
		if (value==null || value.length()==0)
			return;
		byte code = lexiconFile.getOrCreateAttributeCode(attribute, value);
		
		int attributeBit = attributeBitMap.get(attribute);
		
		// count bits to the left of current marker
		int bitCount = 0;
		for (int i=0x1; i<attributeBit; i*=2) {
			if ((attributeMarker & i)>0)
				bitCount++;
		}

		if ((attributeMarker & attributeBit)>0) {
			// replace existing value		
			attributeCodes[bitCount] = code;
		} else {
			// insert new value, means upping all higher values
			byte[] newAttributeCodes = new byte[attributeCodes.length+1];
			for (int i=0; i<newAttributeCodes.length; i++) {
				if (i<bitCount)
					newAttributeCodes[i] = attributeCodes[i];
				else if (i==bitCount)
					newAttributeCodes[i] = code;
				else
					newAttributeCodes[i] = attributeCodes[i-1];
			}
			attributeCodes = newAttributeCodes;
			attributeMarker = (attributeMarker | attributeBit);
		}
	}
	
	private List<String> getValueAsList(LexicalAttribute attribute) {
		String value = this.getValue(attribute);
		if (value.length()==0)
			return new ArrayList<String>();
		
		String[] parts = value.split("\t", -1);
		List<String> list = Arrays.asList(parts);
		return list;
	}
	
	private void addValue(LexicalAttribute attribute, String value) {
		String currentValue = this.getValue(attribute);
		if (currentValue.length()==0)
			currentValue = value;
		else
			currentValue = currentValue + "\t" + value;
		this.setValue(attribute, currentValue);
	}
	
	public String getWord() {
		return word;
	}
	@Override
	public void setWord(String word) {
		this.word = word;
	}
	public String getLemma() {
		return lemma;
	}
	@Override
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	public String getLemmaComplement() {
		return this.getValue(LexicalAttribute.LemmaComplement);
	}
	@Override
	public void setLemmaComplement(String lemmaComplement) {
		this.setValue(LexicalAttribute.LemmaComplement, lemmaComplement);
	}
	
	public String getCategory() {
		return this.getValue(LexicalAttribute.Category);
	}
	@Override
	public void setCategory(String category) {
		this.setValue(LexicalAttribute.Category, category);
	}
	
	public String getMorphology() {
		return this.getValue(LexicalAttribute.Morphology);
	}
	@Override
	public void setMorphology(String morphology) {
		this.setValue(LexicalAttribute.Morphology, morphology);
	}
	
	public String getSubCategory() {
		return this.getValue(LexicalAttribute.SubCategory);
	}
	@Override
	public void setSubCategory(String subCategory) {
		this.setValue(LexicalAttribute.SubCategory, subCategory);
	}
	
	public List<String> getGender() {
		return this.getValueAsList(LexicalAttribute.Gender);
	}
	@Override
	public void addGender(String gender) {
		this.addValue(LexicalAttribute.Gender, gender);
	}
	
	public List<String> getNumber() {
		return this.getValueAsList(LexicalAttribute.Number);
	}
	@Override
	public void addNumber(String number) {
		this.addValue(LexicalAttribute.Number, number);
	}
	
	public List<String> getPerson() {
		return this.getValueAsList(LexicalAttribute.Person);
	}
	@Override
	public void addPerson(String person) {
		this.addValue(LexicalAttribute.Person, person);
	}
		
	public List<String> getTense() {
		return this.getValueAsList(LexicalAttribute.Tense);
	}
	@Override
	public void addTense(String tense) {
		this.addValue(LexicalAttribute.Tense, tense);
	}
	
	public List<String> getMood() {
		return this.getValueAsList(LexicalAttribute.Mood);
	}
	@Override
	public void addMood(String mood) {
		this.addValue(LexicalAttribute.Mood, mood);
	}
	
	public List<String> getAspect() {
		return this.getValueAsList(LexicalAttribute.Aspect);
	}
	@Override
	public void addAspect(String aspect) {
		this.addValue(LexicalAttribute.Aspect, aspect);
	}
	
	public List<String> getCase() {
		return this.getValueAsList(LexicalAttribute.Case);
	}
	@Override
	public void addCase(String grammaticalCase) {
		this.addValue(LexicalAttribute.Case, grammaticalCase);
	}
	
	public List<String> getPossessorNumber() {
		return this.getValueAsList(LexicalAttribute.PossessorNumber);
	}
	@Override
	public void addPossessorNumber(String possessorNumber) {
		this.addValue(LexicalAttribute.PossessorNumber, possessorNumber);
	}
	


	@Override
	public String getAttribute(String attribute) {
		if (attribute.equals(LexicalAttribute.Word.name()))
			return this.word;
		else if (attribute.equals(LexicalAttribute.Lemma.name()))
			return this.lemma;
		
		LexicalAttribute myAttribute = null;
		try {
			myAttribute = LexicalAttribute.valueOf(attribute);
		} catch (IllegalArgumentException e) {
			// do nothing
		}
		if (myAttribute==null) {
			myAttribute = lexiconFile.getAttributeForName(attribute);
		}
		return this.getValue(myAttribute);
	}
	
	@Override
	public void setAttribute(String attribute, String value) {
		if (attribute.equals(LexicalAttribute.Word.name()))
			this.setWord(value);
		else if (attribute.equals(LexicalAttribute.Lemma.name()))
			this.setLemma(value);
		
		LexicalAttribute myAttribute = null;
		try {
			myAttribute = LexicalAttribute.valueOf(attribute);
		} catch (IllegalArgumentException e) {
			// do nothing
		}
		if (myAttribute==null) {
			myAttribute = lexiconFile.getAttributeForName(attribute);
		}
		this.setValue(myAttribute, value);
	}
	
	public String getLexiconName() {
		return this.lexiconFile.getName();
	}
	
	@Override
	public String toString() {
		return "LexicalEntry [lexiconName=" + this.getLexiconName() + ", word="
				+ word + ", lemma=" + lemma + ", lemmaComplement="
				+ this.getLemmaComplement() + ", category=" + this.getCategory() + ", subCategory=" + this.getSubCategory()
				+ ", morphology=" + this.getMorphology() + "]";
	}
}
