///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Joliciel Informatique
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
package com.joliciel.talismane.tokeniser.filters;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.tokeniser.StringAttribute;
import com.joliciel.talismane.tokeniser.TokenAttribute;
import com.joliciel.talismane.utils.StringUtils;

/**
 * A parent class for TokenRegexFilter implementations, which knows how to
 * construct the Pattern using the various attributes set and the initial regex,
 * and how to apply the regex to get the matching placeholders.
 * 
 * @author Assaf Urieli
 *
 */
public abstract class AbstractRegexFilter implements TokenRegexFilter, NeedsTalismaneSession {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractRegexFilter.class);
	private static Pattern wordListPattern = Pattern.compile("\\\\p\\{WordList\\((.*?)\\)\\}", Pattern.UNICODE_CHARACTER_CLASS);
	private static Pattern diacriticPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private String regex;
	private Pattern pattern;
	private int groupIndex = 0;
	private boolean possibleSentenceBoundary = true;
	private Map<String, TokenAttribute<?>> attributes = new HashMap<String, TokenAttribute<?>>();
	private boolean caseSensitive = true;
	private boolean diacriticSensitive = true;
	private boolean autoWordBoundaries = false;
	private boolean excluded = false;

	private TalismaneSession talismaneSession;

	/**
	 * A constructor with the minimum required data. This is the constructor to
	 * be used when creating filters directly in code.
	 * 
	 * @param regex
	 * @param talismaneSession
	 */
	public AbstractRegexFilter(String regex, TalismaneSession talismaneSession) {
		this.regex = regex;
		this.talismaneSession = talismaneSession;
	}

	/**
	 * A constructor used when automatically generating filters from
	 * descriptors.
	 */
	public AbstractRegexFilter() {
	}

	@Override
	public List<TokenPlaceholder> apply(String text) {
		List<TokenPlaceholder> placeholders = new ArrayList<TokenPlaceholder>();

		Matcher matcher = this.getPattern().matcher(text);
		int lastStart = -1;
		while (matcher.find()) {
			int start = matcher.start(groupIndex);
			if (start > lastStart) {
				int end = matcher.end(groupIndex);
				String replacement = this.findReplacement(text, matcher);
				TokenPlaceholder placeholder = new TokenPlaceholder(start, end, replacement, regex);
				placeholder.setPossibleSentenceBoundary(this.possibleSentenceBoundary);
				for (String key : attributes.keySet()) {
					TokenAttribute<?> tokenAttribute = attributes.get(key);
					placeholder.addAttribute(key, tokenAttribute);
				}
				placeholders.add(placeholder);
			}
			lastStart = start;
		}

		return placeholders;
	}

	/**
	 * If the token text should be replaced, return something, otherwise return
	 * null.
	 */
	protected String findReplacement(String text, Matcher matcher) {
		return null;
	}

	@Override
	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Override
	public String toString() {
		return "AbstractRegexFilter [regex=" + regex + "]";
	}

	@Override
	public boolean isPossibleSentenceBoundary() {
		return possibleSentenceBoundary;
	}

	@Override
	public void setPossibleSentenceBoundary(boolean possibleSentenceBoundary) {
		this.possibleSentenceBoundary = possibleSentenceBoundary;
	}

	@Override
	public int getGroupIndex() {
		return groupIndex;
	}

	@Override
	public void setGroupIndex(int groupIndex) {
		this.groupIndex = groupIndex;
	}

	@Override
	public Map<String, TokenAttribute<?>> getAttributes() {
		return attributes;
	}

	@Override
	public void addAttribute(String key, TokenAttribute<?> value) {
		attributes.put(key, value);
	}

	@Override
	public Pattern getPattern() {
		if (pattern == null) {
			// we may need to replace WordLists by the list contents
			String myRegex = this.regex;

			if (LOG.isTraceEnabled()) {
				LOG.trace("Regex: " + myRegex);
			}

			if (this.autoWordBoundaries) {
				Boolean startsWithLetter = null;
				for (int i = 0; i < myRegex.length() && startsWithLetter == null; i++) {
					char c = myRegex.charAt(i);
					if (c == '\\') {
						i++;
						c = myRegex.charAt(i);
						if (c == 'd' || c == 'w') {
							startsWithLetter = true;
						} else if (c == 's' || c == 'W' || c == 'b' || c == 'B') {
							startsWithLetter = false;
						} else if (c == 'p') {
							i += 2; // skip the open curly brackets
							int closeCurlyBrackets = myRegex.indexOf('}', i);
							int openParentheses = myRegex.indexOf('(', i);
							int endIndex = closeCurlyBrackets;
							if (openParentheses > 0 && openParentheses < closeCurlyBrackets)
								endIndex = openParentheses;
							if (endIndex > 0) {
								String specialClass = myRegex.substring(i, endIndex);
								if (specialClass.equals("WordList")) {
									startsWithLetter = true;
								}
							}
							if (startsWithLetter == null)
								startsWithLetter = false;
						}
						break;
					} else if (c == '[' || c == '(') {
						// do nothing
					} else if (Character.isLetter(c) || Character.isDigit(c)) {
						startsWithLetter = true;
					} else {
						startsWithLetter = false;
					}
				}

				Boolean endsWithLetter = null;
				for (int i = myRegex.length() - 1; i >= 0 && endsWithLetter == null; i--) {
					char c = myRegex.charAt(i);
					char prevC = ' ';
					char prevPrevC = ' ';
					if (i >= 1)
						prevC = myRegex.charAt(i - 1);
					if (i >= 2)
						prevPrevC = myRegex.charAt(i - 2);
					if (prevC == '\\' && StringUtils.countChar(myRegex, prevC, i - 1, false) % 2 == 1) {
						// the previous character was an escaping backslash
						if (c == 'd' || c == 'w') {
							endsWithLetter = true;
						} else if (c == 's' || c == 'W' || c == 'b' || c == 'B') {
							endsWithLetter = false;
						} else {
							endsWithLetter = false;
						}
						break;
					} else if (c == ']' || c == ')' || c == '+') {
						// do nothing
					} else if (c == '}') {
						int startIndex = myRegex.lastIndexOf('{') + 1;
						int closeCurlyBrackets = myRegex.indexOf('}', startIndex);
						int openParentheses = myRegex.indexOf('(', startIndex);
						int endIndex = closeCurlyBrackets;
						if (openParentheses > 0 && openParentheses < closeCurlyBrackets)
							endIndex = openParentheses;
						if (endIndex > 0) {
							String specialClass = myRegex.substring(startIndex, endIndex);
							if (specialClass.equals("WordList") || specialClass.equals("Alpha") || specialClass.equals("Lower") || specialClass.equals("Upper")
									|| specialClass.equals("ASCII") || specialClass.equals("Digit")) {
								endsWithLetter = true;
							}
						}
						break;
					} else if (c == '?' || c == '*') {
						if (Character.isLetterOrDigit(prevC)) {
							if (prevPrevC == '\\' && StringUtils.countChar(myRegex, prevPrevC, i - 2, false) % 2 == 1) {
								// the preceding character was an escaping
								// backslash...
								if (prevC == 'd' || prevC == 'w') {
									// skip this construct
									i -= 2;
								} else {
									endsWithLetter = false;
								}
							} else {
								// since the matched text may or may not match
								// prevC
								// we skip this letter and continue, to find out
								// if prior to this letter
								// there's another letter
								i--;
							}
						} else {
							endsWithLetter = false;
						}
					} else if (Character.isLetterOrDigit(c)) {
						endsWithLetter = true;
					} else {
						endsWithLetter = false;
					}
				}

				if (startsWithLetter != null && startsWithLetter) {
					myRegex = "\\b" + myRegex;
				}
				if (endsWithLetter != null && endsWithLetter) {
					myRegex = myRegex + "\\b";
				}
				if (LOG.isTraceEnabled()) {
					LOG.trace("After autoWordBoundaries: " + myRegex);
				}
			}

			if (!this.caseSensitive || !this.diacriticSensitive) {
				StringBuilder regexBuilder = new StringBuilder();
				for (int i = 0; i < myRegex.length(); i++) {
					char c = myRegex.charAt(i);
					if (c == '\\') {
						// escape - skip next
						regexBuilder.append(c);
						i++;
						c = myRegex.charAt(i);
						regexBuilder.append(c);
					} else if (c == '[') {
						// character group, don't change it
						regexBuilder.append(c);
						while (c != ']' && i < myRegex.length()) {
							i++;
							c = myRegex.charAt(i);
							regexBuilder.append(c);
						}
					} else if (c == '{') {
						// command, don't change it
						regexBuilder.append(c);
						while (c != '}' && i < myRegex.length()) {
							i++;
							c = myRegex.charAt(i);
							regexBuilder.append(c);
						}
					} else if (Character.isLetter(c)) {
						Set<String> chars = new TreeSet<String>();
						chars.add("" + c);
						char noAccent = diacriticPattern.matcher(Normalizer.normalize("" + c, Form.NFD)).replaceAll("").charAt(0);

						if (!this.caseSensitive) {
							chars.add("" + Character.toUpperCase(c));
							chars.add("" + Character.toLowerCase(c));
							chars.add("" + Character.toUpperCase(noAccent));
						}
						if (!this.diacriticSensitive) {
							chars.add("" + noAccent);
							if (!this.caseSensitive) {
								chars.add("" + Character.toLowerCase(noAccent));
							}
						}
						if (chars.size() == 1) {
							regexBuilder.append(c);
						} else {
							regexBuilder.append('[');
							for (String oneChar : chars) {
								regexBuilder.append(oneChar);
							}
							regexBuilder.append(']');
						}
					} else {
						regexBuilder.append(c);
					}
				}
				myRegex = regexBuilder.toString();
				if (LOG.isTraceEnabled()) {
					LOG.trace("After caseSensitive: " + myRegex);
				}
			}

			Matcher matcher = wordListPattern.matcher(myRegex);
			StringBuilder regexBuilder = new StringBuilder();

			int lastIndex = 0;
			while (matcher.find()) {
				String[] params = matcher.group(1).split(",");
				int start = matcher.start();
				int end = matcher.end();
				regexBuilder.append(myRegex.substring(lastIndex, start));

				String wordListName = params[0];
				boolean uppercaseOptional = false;
				boolean diacriticsOptional = false;
				boolean lowercaseOptional = false;
				boolean firstParam = true;
				for (String param : params) {
					if (firstParam) {
						/* word list name */ } else if (param.equals("diacriticsOptional"))
						diacriticsOptional = true;
					else if (param.equals("uppercaseOptional"))
						uppercaseOptional = true;
					else if (param.equals("lowercaseOptional"))
						lowercaseOptional = true;
					else
						throw new TalismaneException("Unknown parameter in word list " + matcher.group(1) + ": " + param);
					firstParam = false;
				}

				WordList wordList = talismaneSession.getWordListFinder().getWordList(wordListName);
				if (wordList == null)
					throw new TalismaneException("Unknown word list: " + wordListName);

				StringBuilder sb = new StringBuilder();

				boolean firstWord = true;
				for (String word : wordList.getWordList()) {
					if (!firstWord)
						sb.append("|");
					word = Normalizer.normalize(word, Form.NFC);
					if (uppercaseOptional || diacriticsOptional) {
						String wordNoDiacritics = Normalizer.normalize(word, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
						String wordLowercase = word.toLowerCase(Locale.ENGLISH);
						String wordLowercaseNoDiacritics = Normalizer.normalize(wordLowercase, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
						String wordUppercase = wordNoDiacritics.toUpperCase(Locale.ENGLISH);

						boolean needsGrouping = false;
						if (uppercaseOptional && !word.equals(wordLowercase))
							needsGrouping = true;
						if (diacriticsOptional && !word.equals(wordNoDiacritics))
							needsGrouping = true;
						if (lowercaseOptional && !word.equals(wordUppercase))
							needsGrouping = true;
						if (needsGrouping) {
							for (int i = 0; i < word.length(); i++) {
								char c = word.charAt(i);

								boolean grouped = false;
								if (uppercaseOptional && c != wordLowercase.charAt(i))
									grouped = true;
								if (diacriticsOptional && c != wordNoDiacritics.charAt(i))
									grouped = true;
								if (lowercaseOptional && c != wordUppercase.charAt(i))
									grouped = true;

								if (!grouped)
									sb.append(c);
								else {
									sb.append("[");
									String group = "" + c;
									if (uppercaseOptional && group.indexOf(wordLowercase.charAt(i)) < 0)
										group += (wordLowercase.charAt(i));
									if (lowercaseOptional && group.indexOf(wordUppercase.charAt(i)) < 0)
										group += (wordUppercase.charAt(i));
									if (diacriticsOptional && group.indexOf(wordNoDiacritics.charAt(i)) < 0)
										group += (wordNoDiacritics.charAt(i));
									if (uppercaseOptional && diacriticsOptional && group.indexOf(wordLowercaseNoDiacritics.charAt(i)) < 0)
										group += (wordLowercaseNoDiacritics.charAt(i));

									sb.append(group);
									sb.append("]");
								} // does this letter need grouping?
							} // next letter
						} else {
							sb.append(word);
						} // any options activated?
					} else {
						sb.append(word);
					}
					firstWord = false;
				} // next word in list

				regexBuilder.append(sb.toString());
				lastIndex = end;
			} // next match
			regexBuilder.append(myRegex.substring(lastIndex));
			myRegex = regexBuilder.toString();
			this.pattern = Pattern.compile(myRegex, Pattern.UNICODE_CHARACTER_CLASS);
		}
		return pattern;
	}

	@Override
	public boolean isDiacriticSensitive() {
		return diacriticSensitive;
	}

	@Override
	public void setDiacriticSensitive(boolean diacriticSensitive) {
		this.diacriticSensitive = diacriticSensitive;
	}

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	@Override
	public boolean isAutoWordBoundaries() {
		return autoWordBoundaries;
	}

	@Override
	public void setAutoWordBoundaries(boolean autoWordBoundaries) {
		this.autoWordBoundaries = autoWordBoundaries;
	}

	@Override
	public void verify() {
		if (regex == null || regex.length() == 0)
			throw new TalismaneException("Cannot use an empty regex for a filter");

		Pattern pattern = this.getPattern();
		Matcher matcher = pattern.matcher("");
		if (this.groupIndex > matcher.groupCount()) {
			throw new TalismaneException("No group " + this.groupIndex + " in pattern: " + this.regex);
		}
	}

	@Override
	public void load(Map<String, String> parameters, List<String> tabs) throws TokenFilterLoadException {
		if (tabs.size() < 1)
			throw new TokenFilterLoadException(
					"Wrong number of additional tabs for " + TokenRegexFilter.class.getSimpleName() + ". Expected at least 1, but was " + tabs.size());
		this.setRegex(tabs.get(0));

		this.loadInternal(parameters, tabs);

		for (String paramName : parameters.keySet()) {
			String paramValue = parameters.get(paramName);
			if (paramName.equals("possibleSentenceBoundary")) {
				this.setPossibleSentenceBoundary(Boolean.valueOf(paramValue));
			} else if (paramName.equals("group")) {
				this.setGroupIndex(Integer.parseInt(paramValue));
			} else if (paramName.equals("caseSensitive")) {
				this.setCaseSensitive(Boolean.valueOf(paramValue));
			} else if (paramName.equals("diacriticSensitive")) {
				this.setDiacriticSensitive(Boolean.valueOf(paramValue));
			} else if (paramName.equals("autoWordBoundaries")) {
				this.setAutoWordBoundaries(Boolean.valueOf(paramValue));
			} else {
				this.addAttribute(paramName, new StringAttribute(paramValue));
			}
		}

		this.verify();
	}

	/**
	 * Load the token filter's state using information extracted from a
	 * descriptor.
	 * 
	 * @param parameters
	 *            a series of name/value parameters. Subclasses should remove
	 *            any parameters handled before passing on to the superclass.
	 * @param tabs
	 *            a list of unnamed parameters, whose placement determines their
	 *            meaning
	 */
	protected abstract void loadInternal(Map<String, String> parameters, List<String> tabs) throws TokenFilterLoadException;

	@Override
	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	@Override
	public TalismaneSession getTalismaneSession() {
		return talismaneSession;
	}

	@Override
	public void setTalismaneSession(TalismaneSession talismaneSession) {
		this.talismaneSession = talismaneSession;
	}
}