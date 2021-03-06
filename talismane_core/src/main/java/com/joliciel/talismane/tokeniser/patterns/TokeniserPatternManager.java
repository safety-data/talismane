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
package com.joliciel.talismane.tokeniser.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.tokeniser.SeparatorDecision;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.Tokeniser;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;

/**
 * The TokeniserPatternManager will read patterns from a list of strings,
 * typically stored in a file.<br/>
 * <br/>
 * The list of strings should contain default decisions for the various
 * separators, in lines as follows:<br/>
 * IS_NOT_SEPARATOR -<br/>
 * IS_SEPARATOR_AFTER '<br/>
 * IS_SEPARATOR_BEFORE<br/>
 * All other separators are assumed to separate tokens on both sides
 * (IS_SEPARATOR)<br/>
 * <br/>
 * Next, it should contain a list of patterns.<br/>
 * Optionally, each pattern can be preceded by a user-friendly name and a tab.
 * <br/>
 * Patterns are used to check if any set of n atomic tokens in a sentence
 * matches it. If so, all of the separators inside the set are tested further.
 * <br/>
 * More information on patterns can be found in the {@link TokenPattern} class.
 * <br/>
 * 
 * @author Assaf Urieli
 *
 */
public class TokeniserPatternManager {
	private static final Logger LOG = LoggerFactory.getLogger(TokeniserPatternManager.class);

	private Map<SeparatorDecision, String> separatorDefaults;
	private Map<SeparatorDecision, Pattern> separatorDefaultPatterns;
	private List<String> descriptors;
	private List<String> testPatterns;

	private List<TokenPattern> parsedTestPatterns;

	/**
	 * Reads separator defaults and test patterns from a list of strings.
	 */
	public TokeniserPatternManager(List<String> patternDescriptors) {
		this.descriptors = patternDescriptors;
		this.configure(patternDescriptors);
	}

	private void configure(List<String> patternDescriptors) {
		String[] separatorDecisions = new String[] { SeparatorDecision.IS_NOT_SEPARATOR.toString(), SeparatorDecision.IS_SEPARATOR_AFTER.toString(),
				SeparatorDecision.IS_SEPARATOR_BEFORE.toString() };
		List<String> testPatterns = new ArrayList<String>();
		Map<SeparatorDecision, String> separatorDefaults = new HashMap<SeparatorDecision, String>();
		for (String line : patternDescriptors) {
			if (line.startsWith("#"))
				continue;
			boolean lineProcessed = false;
			for (String separatorDecision : separatorDecisions) {
				if (line.startsWith(separatorDecision)) {
					if (line.length() > separatorDecision.length() + 1) {
						String separatorsForDefault = line.substring(separatorDecision.length() + 1);
						if (LOG.isTraceEnabled())
							LOG.trace(separatorDecision + ": '" + separatorsForDefault + "'");
						if (separatorsForDefault.length() > 0)
							separatorDefaults.put(Enum.valueOf(SeparatorDecision.class, separatorDecision), separatorsForDefault);
					}
					lineProcessed = true;
					break;
				}
			}
			if (lineProcessed)
				continue;
			if (line.trim().length() > 0)
				testPatterns.add(line.trim());
		}
		this.separatorDefaults = separatorDefaults;
		this.testPatterns = testPatterns;
	}

	/**
	 * For each type of separator decision, a list of separators (e.g.
	 * punctuation marks) which will receive this decision by default.
	 */
	public Map<SeparatorDecision, String> getSeparatorDefaults() {
		return separatorDefaults;
	}

	/**
	 * The test patterns - only token sequences matching these patterns will be
	 * submitted to further decision.
	 */
	public List<String> getTestPatterns() {
		return testPatterns;
	}

	/**
	 * Test patterns after parsing.
	 */
	public List<TokenPattern> getParsedTestPatterns() {
		if (this.parsedTestPatterns == null && this.testPatterns != null) {
			this.parsedTestPatterns = new ArrayList<TokenPattern>();
			for (String testPattern : this.testPatterns) {
				String pattern = testPattern;
				String name = null;
				String groupName = null;
				String[] parts = testPattern.split("\t");
				if (parts.length == 2) {
					name = parts[0];
					pattern = parts[1];
				} else if (parts.length == 3) {
					name = parts[0];
					groupName = parts[1];
					pattern = parts[2];
				}

				TokenPattern parsedPattern = new TokenPattern(pattern, Tokeniser.SEPARATORS);
				if (name != null)
					parsedPattern.setName(name);
				if (groupName != null)
					parsedPattern.setGroupName(groupName);
				this.parsedTestPatterns.add(parsedPattern);
			}
		}
		return parsedTestPatterns;
	}

	/**
	 * Takes a sequence of atomic tokens and applies default decisions for each
	 * separator.
	 */
	public List<TokeniserOutcome> getDefaultOutcomes(TokenSequence tokenSequence) {
		List<TokeniserOutcome> defaultOutcomes = new ArrayList<TokeniserOutcome>();

		// Assign each separator its default value
		TokeniserOutcome nextOutcome = TokeniserOutcome.SEPARATE;
		for (Token token : tokenSequence.listWithWhiteSpace()) {

			TokeniserOutcome outcome = null;
			if (Tokeniser.SEPARATORS.matcher(token.getText()).matches()) {
				boolean defaultValueFound = false;
				for (Entry<SeparatorDecision, Pattern> entry : this.getSeparatorDefaultPatterns().entrySet()) {
					if (entry.getValue().matcher(token.getText()).matches()) {
						defaultValueFound = true;
						SeparatorDecision defaultSeparatorDecision = entry.getKey();
						switch (defaultSeparatorDecision) {
						case IS_SEPARATOR:
							outcome = TokeniserOutcome.SEPARATE;
							nextOutcome = TokeniserOutcome.SEPARATE;
							break;
						case IS_NOT_SEPARATOR:
							outcome = TokeniserOutcome.JOIN;
							nextOutcome = TokeniserOutcome.JOIN;
							break;
						case IS_SEPARATOR_BEFORE:
							outcome = TokeniserOutcome.SEPARATE;
							nextOutcome = TokeniserOutcome.JOIN;
						case IS_SEPARATOR_AFTER:
							outcome = TokeniserOutcome.JOIN;
							nextOutcome = TokeniserOutcome.SEPARATE;
						case NOT_APPLICABLE:
							break;
						default:
							break;
						}
						break;
					}
				}
				if (!defaultValueFound) {
					outcome = TokeniserOutcome.SEPARATE;
					nextOutcome = TokeniserOutcome.SEPARATE;
				}
				defaultOutcomes.add(outcome);
			} else {
				defaultOutcomes.add(nextOutcome);
			}

		}
		return defaultOutcomes;
	}

	public void setSeparatorDefaults(Map<SeparatorDecision, String> separatorDefaults) {
		this.separatorDefaults = separatorDefaults;
	}

	protected Map<SeparatorDecision, Pattern> getSeparatorDefaultPatterns() {
		if (this.separatorDefaultPatterns == null) {
			this.separatorDefaultPatterns = new HashMap<SeparatorDecision, Pattern>();
			for (Entry<SeparatorDecision, String> entry : this.getSeparatorDefaults().entrySet()) {
				String separators = entry.getValue();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < separators.length(); i++) {
					char c = separators.charAt(i);
					sb.append('\\');
					sb.append(c);
				}
				Pattern pattern = Pattern.compile("[" + sb.toString() + "]", Pattern.UNICODE_CHARACTER_CLASS);
				this.separatorDefaultPatterns.put(entry.getKey(), pattern);
			}

		}
		return separatorDefaultPatterns;
	}

	/**
	 * The full list of descriptors used to construct this pattern manager.
	 */
	public List<String> getDescriptors() {
		return descriptors;
	}

}
