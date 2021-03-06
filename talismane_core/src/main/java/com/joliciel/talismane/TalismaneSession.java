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
package com.joliciel.talismane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.joliciel.talismane.lexicon.Diacriticizer;
import com.joliciel.talismane.lexicon.EmptyLexicon;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.resources.WordListFinder;

/**
 * A class storing session-wide reference data.
 * 
 * @author Assaf Urieli
 *
 */
public class TalismaneSession {
	private static final Map<String, TalismaneSession> instances = new HashMap<>();

	private final String sessionId;
	private Locale locale;
	private PosTagSet posTagSet;
	private List<PosTaggerLexicon> lexicons = new ArrayList<PosTaggerLexicon>();
	private PosTaggerLexicon mergedLexicon;
	private TransitionSystem transitionSystem;
	private LinguisticRules linguisticRules;
	private Diacriticizer diacriticizer;
	private String outputDivider = "";
	private final WordListFinder wordListFinder = new WordListFinder();
	private final ExternalResourceFinder externalResourceFinder = new ExternalResourceFinder();

	public static TalismaneSession getInstance(String sessionId) {
		TalismaneSession session = instances.get(sessionId);
		if (session == null) {
			session = new TalismaneSession(sessionId);
			instances.put(sessionId, session);
		}
		return session;
	}

	private TalismaneSession(String sessionId) {
		this.sessionId = sessionId;
	}

	public synchronized PosTagSet getPosTagSet() {
		if (posTagSet == null)
			throw new TalismaneException("PosTagSet missing.");
		return posTagSet;
	}

	public void setPosTagSet(PosTagSet posTagSet) {
		this.posTagSet = posTagSet;
	}

	public synchronized TransitionSystem getTransitionSystem() {
		if (transitionSystem == null)
			throw new TalismaneException("TransitionSystem missing.");
		return transitionSystem;
	}

	public void setTransitionSystem(TransitionSystem transitionSystem) {
		this.transitionSystem = transitionSystem;
	}

	/**
	 * A list of lexicons setup for the current session.
	 */
	public synchronized List<PosTaggerLexicon> getLexicons() {
		return lexicons;
	}

	public void addLexicon(PosTaggerLexicon lexicon) {
		this.lexicons.add(lexicon);
	}

	public Locale getLocale() {
		if (locale == null && this.getPosTagSet() != null) {
			locale = this.getPosTagSet().getLocale();
		}
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public synchronized LinguisticRules getLinguisticRules() {
		if (linguisticRules == null) {
			linguisticRules = new GenericRules(this);
		}
		return linguisticRules;
	}

	public void setLinguisticRules(LinguisticRules linguisticRules) {
		this.linguisticRules = linguisticRules;
	}

	/**
	 * Get a lexicon which merges all of the lexicons added, prioritised in the
	 * order in which they were added.
	 */
	public synchronized PosTaggerLexicon getMergedLexicon() {
		if (mergedLexicon == null) {
			List<PosTaggerLexicon> lexicons = this.getLexicons();
			if (lexicons.size() == 0)
				mergedLexicon = new EmptyLexicon();
			else if (lexicons.size() == 1)
				mergedLexicon = lexicons.get(0);
			else {
				LexiconChain lexiconChain = new LexiconChain();
				for (PosTaggerLexicon lexicon : lexicons) {
					lexiconChain.addLexicon(lexicon);
				}
				mergedLexicon = lexiconChain;
			}
		}
		return mergedLexicon;
	}

	public Diacriticizer getDiacriticizer() {
		if (diacriticizer == null) {
			diacriticizer = new Diacriticizer(this.getMergedLexicon());
			diacriticizer.setLocale(this.getLocale());
		}
		return diacriticizer;
	}

	public void setDiacriticizer(Diacriticizer diacriticizer) {
		this.diacriticizer = diacriticizer;
	}

	/**
	 * A string inserted between outputs (such as a newline).
	 */

	public String getOutputDivider() {
		return outputDivider;
	}

	public void setOutputDivider(String outputDivider) {
		this.outputDivider = outputDivider;
	}

	public WordListFinder getWordListFinder() {
		return wordListFinder;
	}

	public ExternalResourceFinder getExternalResourceFinder() {
		return externalResourceFinder;
	}

	public String getSessionId() {
		return sessionId;
	}
}
