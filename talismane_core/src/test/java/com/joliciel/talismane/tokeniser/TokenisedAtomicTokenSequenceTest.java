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
package com.joliciel.talismane.tokeniser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.machineLearning.Decision;

import mockit.NonStrict;
import mockit.NonStrictExpectations;

public class TokenisedAtomicTokenSequenceTest {
	private static final Logger LOG = LoggerFactory.getLogger(TokenisedAtomicTokenSequenceTest.class);

	@Test
	public void testGetTokenSequence(@NonStrict final Sentence sentence) {
		new NonStrictExpectations() {
			{
				sentence.getText();
				returns("Je n'ai pas encore l'ourang-outan.");
			}
		};

		TokeniserOutcome[] tokeniserOutcomeArray = new TokeniserOutcome[] { TokeniserOutcome.SEPARATE, // Je
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // n
				TokeniserOutcome.JOIN, // '
				TokeniserOutcome.SEPARATE, // ai
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // pas
				TokeniserOutcome.JOIN, // _
				TokeniserOutcome.JOIN, // encore
				TokeniserOutcome.SEPARATE, // _
				TokeniserOutcome.SEPARATE, // l
				TokeniserOutcome.JOIN, // '
				TokeniserOutcome.SEPARATE, // ourang
				TokeniserOutcome.JOIN, // -
				TokeniserOutcome.JOIN, // outan
				TokeniserOutcome.SEPARATE // .
		};

		final TalismaneSession talismaneSession = TalismaneSession.getInstance("");

		TokenisedAtomicTokenSequence atomicTokenSequence = new TokenisedAtomicTokenSequence(sentence, talismaneSession);

		TokenSequence tokenSequence = new TokenSequence(sentence, Tokeniser.SEPARATORS, talismaneSession);

		int i = 0;
		for (Token token : tokenSequence.listWithWhiteSpace()) {
			Decision decision = new Decision(tokeniserOutcomeArray[i++].name());
			TaggedToken<TokeniserOutcome> taggedToken = new TaggedToken<>(token, decision, TokeniserOutcome.valueOf(decision.getOutcome()));

			atomicTokenSequence.add(taggedToken);
		}

		TokenSequence newTokenSequence = atomicTokenSequence.inferTokenSequence();
		LOG.debug(newTokenSequence.toString());

		i = 0;
		for (Token token : newTokenSequence) {
			if (i == 0) {
				assertEquals("Je", token.getText());
			} else if (i == 1) {
				assertEquals("n'", token.getText());
			} else if (i == 2) {
				assertEquals("ai", token.getText());
			} else if (i == 3) {
				assertEquals("pas encore", token.getText());
			} else if (i == 4) {
				assertEquals("l'", token.getText());
			} else if (i == 5) {
				assertEquals("ourang-outan", token.getText());
			} else if (i == 6) {
				assertEquals(".", token.getText());
			}
			i++;
		}
		assertEquals(7, newTokenSequence.size());
	}
}
