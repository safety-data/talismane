package com.joliciel.talismane.tokeniser.filters;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.resources.WordList;
import com.joliciel.talismane.tokeniser.StringAttribute;

public class TokenRegexFilterImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenRegexFilterImplTest.class);

	@Before
	public void setup() {
	}

	@Test
	public void testApply() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b[\\w.%-]+@[-.\\w]+\\.[A-Za-z]{2,4}\\b");
		filter.setReplacement("Email");
		String text = "My address is joe.schmoe@test.com.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(14, placeholder.getStartIndex());
		assertEquals(33, placeholder.getEndIndex());
		assertEquals("Email", placeholder.getReplacement());
	}

	@Test
	public void testApplyWithDollars() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2:$1");
		String text = "My address is joe.schmoe@test.com.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(14, placeholder.getStartIndex());
		assertEquals(33, placeholder.getEndIndex());
		assertEquals("$Email@test.com:joe.schmoe", placeholder.getReplacement());
	}

	@Test
	public void testApplyWithConsecutiveDollars() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b([\\w.%-]+)(@[-.\\w]+\\.[A-Za-z]{2,4})\\b");
		filter.setReplacement("\\$Email$2$1");
		String text = "My address is joe.schmoe@test.com.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(14, placeholder.getStartIndex());
		assertEquals(33, placeholder.getEndIndex());
		assertEquals("$Email@test.comjoe.schmoe", placeholder.getReplacement());
	}

	@Test
	public void testApplyWithUnmatchingGroups() {
		TokenRegexFilterImpl filter = new TokenRegexFilterImpl();
		filter.setRegex("\\b(\\d)(\\d)?\\b");
		filter.setReplacement("Number$1$2");
		String text = "Two-digit number: 42. One-digit number: 7.";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		System.out.println(placeholders);
		assertEquals(2, placeholders.size());

		TokenPlaceholder placeholder = placeholders.get(0);
		assertEquals("Two-digit number: ".length(), placeholder.getStartIndex());
		assertEquals("Two-digit number: 42".length(), placeholder.getEndIndex());
		assertEquals("Number42", placeholder.getReplacement());
		placeholder = placeholders.get(1);
		assertEquals("Two-digit number: 42. One-digit number: ".length(), placeholder.getStartIndex());
		assertEquals("Two-digit number: 42. One-digit number: 7".length(), placeholder.getEndIndex());
		assertEquals("Number7", placeholder.getReplacement());
	}

	@Test
	public void testWordList() {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");


		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel|Joëlle|Édouard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListDiacriticsOptional() {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel|Jo[ëe]lle|[ÉE]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseOptional() {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,uppercaseOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hloé|[Mm]arcel|[Jj]oëlle|[Éé]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testWordListUppercaseDiacriticsOptional() {
		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");
		wordList.add("Joëlle");
		wordList.add("Édouard");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("\\b(\\p{WordList(FirstNames,diacriticsOptional,uppercaseOptional)}) [A-Z]\\w+\\b");

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b([Cc]hlo[ée]|[Mm]arcel|[Jj]o[ëe]lle|[ÉéEe]douard) [A-Z]\\w+\\b", pattern.pattern());
	}

	@Test
	public void testAutoWordBoundaries() {
		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("hello 123");
		filter.setAutoWordBoundaries(true);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhello 123\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\sabc");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\sabc\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\bblah di blah\\b");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bblah di blah\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("helloe?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bhelloe?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?s?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("lis#e?s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\blis#e?s?", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?\\d?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\d?\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("liste?\\s?");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\bliste?\\s?", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("a\\\\b");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\ba\\\\b\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("\\d+ \\D+");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b\\d+ \\D+", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("abc [A-Z]\\w+");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\babc [A-Z]\\w+\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("(MLLE\\.|Mlle\\.)");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(MLLE\\.|Mlle\\.)", pattern.pattern());

		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("Chloé");
		wordList.add("Marcel");

		WordList nameList = new WordList("FirstNames", wordList);
		talismaneSession.getWordListFinder().addWordList(nameList);

		filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("(\\p{WordList(FirstNames)})");
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chloé|Marcel)\\b", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("(\\p{WordList(FirstNames,diacriticsOptional)}) +([A-Z]'\\p{Alpha}+)");
		filter.setTalismaneSession(talismaneSession);
		filter.setAutoWordBoundaries(true);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("\\b(Chlo[ée]|Marcel) +([A-Z]'\\p{Alpha}+)\\b", pattern.pattern());

	}

	@Test
	public void testCaseSensitive() {

		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setCaseSensitive(false);

		Pattern pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("[Hh][EÉé]", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setDiacriticSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("h[eé]", pattern.pattern());

		filter = new TokenRegexFilterImpl();
		filter.setRegex("hé");
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("[Hh][EeÉé]", pattern.pattern());

		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		final List<String> wordList = new ArrayList<String>();
		wordList.add("apples");
		wordList.add("oranges");
		WordList fruitList = new WordList("Fruit", wordList);
		talismaneSession.getWordListFinder().addWordList(fruitList);

		filter = new TokenRegexFilterImpl();
		filter.setTalismaneSession(talismaneSession);
		filter.setRegex("(\\p{WordList(Fruit)})hé\\w+\\b");
		filter.setDiacriticSensitive(false);
		filter.setCaseSensitive(false);

		pattern = filter.getPattern();
		LOG.debug(pattern.pattern());

		assertEquals("(apples|oranges)[Hh][EeÉé]\\w+\\b", pattern.pattern());

	}

	@Test
	public void testStartOfInput() {
		AbstractRegexFilter filter = new TokenRegexFilterImpl();
		filter.setRegex("^Résumé\\.");
		filter.addAttribute("TAG", new StringAttribute("skip"));
		String text = "Résumé. Résumé des attaques";
		List<TokenPlaceholder> placeholders = filter.apply(text);
		LOG.debug(placeholders.toString());
		assertEquals(1, placeholders.size());
		TokenPlaceholder placeholder = placeholders.iterator().next();
		assertEquals(0, placeholder.getStartIndex());
		assertEquals(7, placeholder.getEndIndex());
		assertEquals(1, placeholder.getAttributes().size());
		assertEquals("TAG", placeholder.getAttributes().keySet().iterator().next());
	}
}
