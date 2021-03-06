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
package com.joliciel.talismane.posTagger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.filters.TokenFilter;
import com.joliciel.talismane.tokeniser.filters.TokenFilterWrapper;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.CoNLLFormatter;

/**
 * A corpus reader that expects one pos-tagged token per line, and analyses the
 * line content based on a regex supplied during construction.<br/>
 * The regex needs to contain the following two capturing groups, indicated by
 * the following strings:<br/>
 * <li>%TOKEN%: the token - note that we assume CoNLL formatting (with
 * underscores for spaces and for empty tokens). The sequence &amp;und; should
 * be used for true underscores.</li>
 * <li>%POSTAG%: the token's pos-tag</li> It can optionally contain the
 * following capturing groups as well:<br/>
 * <li>%FILENAME%: the file containing the token</li>
 * <li>%ROW%: the row containing the token</li>
 * <li>%COLUMN%: the column containing the token</li> The token placeholder will
 * be replaced by (.*). Other placeholders will be replaced by (.+) meaning no
 * empty strings allowed.
 * 
 * @author Assaf Urieli
 *
 */
public class PosTagRegexBasedCorpusReader implements PosTagAnnotatedCorpusReader {
	private static final Logger LOG = LoggerFactory.getLogger(PosTagRegexBasedCorpusReader.class);

	private static final String TOKEN_PLACEHOLDER = "%TOKEN%";
	private static final String POSTAG_PLACEHOLDER = "%POSTAG%";
	private static final String FILENAME_PLACEHOLDER = "%FILENAME%";
	private static final String ROW_PLACEHOLDER = "%ROW%";
	private static final String COLUMN_PLACEHOLDER = "%COLUMN%";

	private PosTagSequence posTagSequence = null;
	private final List<TokenFilter> tokenFilters = new ArrayList<>();
	private final List<TokenSequenceFilter> tokenSequenceFilters = new ArrayList<>();
	private final List<PosTagSequenceFilter> posTagSequenceFilters = new ArrayList<>();
	private TokenSequenceFilter tokenFilterWrapper = null;

	private int lineNumber = 0;
	private int maxSentenceCount = 0;
	private int startSentence = 0;
	private int sentenceCount = 0;
	private int includeIndex = -1;
	private int excludeIndex = -1;
	private int crossValidationSize = 0;

	private final Map<String, Integer> placeholderIndexMap = new HashMap<>();

	private final String regex;
	private final Pattern pattern;
	private final Scanner scanner;
	private final TalismaneSession talismaneSession;

	public PosTagRegexBasedCorpusReader(String regex, Reader reader, TalismaneSession talismaneSession) {
		this.regex = regex;
		this.scanner = new Scanner(reader);
		this.talismaneSession = talismaneSession;

		// construct the input regex
		int tokenPos = regex.indexOf(TOKEN_PLACEHOLDER);
		if (tokenPos < 0)
			throw new TalismaneException("The regex must contain the string \"" + TOKEN_PLACEHOLDER + "\"");

		int posTagPos = regex.indexOf(POSTAG_PLACEHOLDER);
		if (posTagPos < 0)
			throw new TalismaneException("The regex must contain the string \"" + POSTAG_PLACEHOLDER + "\"");

		int filenamePos = regex.indexOf(FILENAME_PLACEHOLDER);
		int rowNumberPos = regex.indexOf(ROW_PLACEHOLDER);
		int columnNumberPos = regex.indexOf(COLUMN_PLACEHOLDER);
		Map<Integer, String> placeholderMap = new TreeMap<Integer, String>();
		placeholderMap.put(tokenPos, TOKEN_PLACEHOLDER);
		placeholderMap.put(posTagPos, POSTAG_PLACEHOLDER);

		if (filenamePos >= 0)
			placeholderMap.put(filenamePos, FILENAME_PLACEHOLDER);
		if (rowNumberPos >= 0)
			placeholderMap.put(rowNumberPos, ROW_PLACEHOLDER);
		if (columnNumberPos >= 0)
			placeholderMap.put(columnNumberPos, COLUMN_PLACEHOLDER);

		int i = 1;
		for (String placeholderName : placeholderMap.values()) {
			placeholderIndexMap.put(placeholderName, i++);
		}

		String regexWithGroups = regex.replace(TOKEN_PLACEHOLDER, "(.*?)");
		regexWithGroups = regexWithGroups.replace(POSTAG_PLACEHOLDER, "(.+?)");
		regexWithGroups = regexWithGroups.replace(FILENAME_PLACEHOLDER, "(.+?)");
		regexWithGroups = regexWithGroups.replace(ROW_PLACEHOLDER, "(.+?)");
		regexWithGroups = regexWithGroups.replace(COLUMN_PLACEHOLDER, "(.+?)");

		this.pattern = Pattern.compile(regexWithGroups, Pattern.UNICODE_CHARACTER_CLASS);
	}

	@Override
	public boolean hasNextPosTagSequence() {
		if (maxSentenceCount > 0 && sentenceCount >= maxSentenceCount) {
			// we've reached the end, do nothing
		} else {
			while (posTagSequence == null) {
				PretokenisedSequence tokenSequence = null;
				List<PosTag> posTags = null;
				boolean hasLine = false;
				if (!scanner.hasNextLine())
					break;
				while ((scanner.hasNextLine() || hasLine) && posTagSequence == null) {
					String line = "";
					if (scanner.hasNextLine())
						line = scanner.nextLine().replace("\r", "");
					lineNumber++;
					if (line.length() == 0) {
						if (!hasLine)
							continue;

						// end of sentence

						boolean includeMe = true;

						// check cross-validation
						if (crossValidationSize > 0) {
							if (includeIndex >= 0) {
								if (sentenceCount % crossValidationSize != includeIndex) {
									includeMe = false;
								}
							} else if (excludeIndex >= 0) {
								if (sentenceCount % crossValidationSize == excludeIndex) {
									includeMe = false;
								}
							}
						}

						if (startSentence > sentenceCount) {
							includeMe = false;
						}

						sentenceCount++;
						LOG.debug("sentenceCount: " + sentenceCount);

						if (!includeMe) {
							hasLine = false;
							tokenSequence = null;
							posTags = null;
							continue;
						}

						tokenSequence.cleanSlate();

						// first apply the token filters - which might replace
						// the text of an individual token
						// with something else
						if (tokenFilterWrapper == null) {
							tokenFilterWrapper = new TokenFilterWrapper(this.tokenFilters);
						}
						tokenFilterWrapper.apply(tokenSequence);

						for (TokenSequenceFilter tokenSequenceFilter : this.tokenSequenceFilters) {
							tokenSequenceFilter.apply(tokenSequence);
						}

						posTagSequence = new PosTagSequence(tokenSequence);
						int i = 0;
						for (PosTag posTag : posTags) {
							Token token = tokenSequence.get(i++);
							if (tokenSequence.getTokensAdded().contains(token)) {
								Decision nullDecision = new Decision(PosTag.NULL_POS_TAG.getCode());
								PosTaggedToken emptyToken = new PosTaggedToken(token, nullDecision, talismaneSession);
								posTagSequence.addPosTaggedToken(emptyToken);
								token = tokenSequence.get(i++);
							}
							Decision corpusDecision = new Decision(posTag.getCode());
							PosTaggedToken posTaggedToken = new PosTaggedToken(token, corpusDecision, talismaneSession);
							posTagSequence.addPosTaggedToken(posTaggedToken);
						}

						for (PosTagSequenceFilter posTagSequenceFilter : this.posTagSequenceFilters) {
							posTagSequenceFilter.apply(posTagSequence);
						}
					} else {
						hasLine = true;

						if (tokenSequence == null) {
							tokenSequence = new PretokenisedSequence(talismaneSession);
							posTags = new ArrayList<PosTag>();
						}

						Matcher matcher = this.getPattern().matcher(line);
						if (!matcher.matches()) {
							throw new TalismaneException(
									"Didn't match pattern on line " + lineNumber + ": Pattern=" + this.getPattern().pattern() + ", Line: " + line);
						}

						if (matcher.groupCount() < placeholderIndexMap.size()) {
							throw new TalismaneException("Expected at least " + placeholderIndexMap.size() + " matches (but found " + matcher.groupCount()
									+ ") on line " + lineNumber);
						}

						String word = matcher.group(placeholderIndexMap.get(TOKEN_PLACEHOLDER));
						word = CoNLLFormatter.fromCoNLL(word);
						Token token = tokenSequence.addToken(word);
						String posTagCode = matcher.group(placeholderIndexMap.get(POSTAG_PLACEHOLDER));
						if (placeholderIndexMap.containsKey(FILENAME_PLACEHOLDER))
							token.setFileName(matcher.group(placeholderIndexMap.get(FILENAME_PLACEHOLDER)));
						if (placeholderIndexMap.containsKey(ROW_PLACEHOLDER))
							token.setLineNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(ROW_PLACEHOLDER))));
						if (placeholderIndexMap.containsKey(COLUMN_PLACEHOLDER))
							token.setColumnNumber(Integer.parseInt(matcher.group(placeholderIndexMap.get(COLUMN_PLACEHOLDER))));

						PosTagSet posTagSet = talismaneSession.getPosTagSet();
						PosTag posTag = null;
						try {
							posTag = posTagSet.getPosTag(posTagCode);
						} catch (UnknownPosTagException upte) {
							throw new TalismaneException("Unknown posTag on line " + lineNumber + ": " + posTagCode);
						}
						posTags.add(posTag);
					}
				}
			}
		}
		return (posTagSequence != null);
	}

	@Override
	public PosTagSequence nextPosTagSequence() {
		PosTagSequence nextSentence = null;
		if (this.hasNextPosTagSequence()) {
			nextSentence = posTagSequence;
			posTagSequence = null;
		}
		return nextSentence;
	}

	@Override
	public void addTokenSequenceFilter(TokenSequenceFilter tokenFilter) {
		this.tokenSequenceFilters.add(tokenFilter);
	}

	public String getRegex() {
		return regex;
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public void addPosTagSequenceFilter(PosTagSequenceFilter posTagSequenceFilter) {
		this.posTagSequenceFilters.add(posTagSequenceFilter);
	}

	@Override
	public int getMaxSentenceCount() {
		return maxSentenceCount;
	}

	@Override
	public void setMaxSentenceCount(int maxSentenceCount) {
		this.maxSentenceCount = maxSentenceCount;
	}

	@Override
	public int getIncludeIndex() {
		return includeIndex;
	}

	@Override
	public void setIncludeIndex(int includeIndex) {
		this.includeIndex = includeIndex;
	}

	@Override
	public int getExcludeIndex() {
		return excludeIndex;
	}

	@Override
	public void setExcludeIndex(int excludeIndex) {
		this.excludeIndex = excludeIndex;
	}

	@Override
	public int getCrossValidationSize() {
		return crossValidationSize;
	}

	@Override
	public void setCrossValidationSize(int crossValidationSize) {
		this.crossValidationSize = crossValidationSize;
	}

	@Override
	public Map<String, String> getCharacteristics() {
		Map<String, String> attributes = new LinkedHashMap<String, String>();

		attributes.put("maxSentenceCount", "" + this.maxSentenceCount);
		attributes.put("crossValidationSize", "" + this.crossValidationSize);
		attributes.put("includeIndex", "" + this.includeIndex);
		attributes.put("excludeIndex", "" + this.excludeIndex);
		attributes.put("tagset", talismaneSession.getPosTagSet().getName());

		int i = 0;
		for (TokenSequenceFilter tokenFilter : this.tokenSequenceFilters) {
			attributes.put("TokenSequenceFilter" + i, "" + tokenFilter.getClass().getSimpleName());

			i++;
		}
		i = 0;
		for (PosTagSequenceFilter posTagSequenceFilter : this.posTagSequenceFilters) {
			attributes.put("PosTagSequenceFilter" + i, "" + posTagSequenceFilter.getClass().getSimpleName());

			i++;
		}
		return attributes;
	}

	@Override
	public int getStartSentence() {
		return startSentence;
	}

	@Override
	public void setStartSentence(int startSentence) {
		this.startSentence = startSentence;
	}

	public boolean hasNextTokenSequence() {
		return this.hasNextPosTagSequence();
	}

	public TokenSequence nextTokenSequence() {
		TokenSequence tokenSequence = this.nextPosTagSequence().getTokenSequence();
		return tokenSequence;
	}

	public void addTokenFilter(TokenFilter tokenFilter) {
		this.tokenFilters.add(tokenFilter);
	}

	public List<TokenSequenceFilter> getTokenSequenceFilters() {
		return tokenSequenceFilters;
	}

	public List<TokenFilter> getTokenFilters() {
		return tokenFilters;
	}

	public boolean hasNextSentence() {
		return this.hasNextPosTagSequence();
	}

	public String nextSentence() {
		return this.nextTokenSequence().getText();
	}

	public boolean isNewParagraph() {
		return false;
	}
}
