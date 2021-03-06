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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.filters.Sentence;

/**
 * A token sequence that has been pre-tokenised by another source (manual
 * annotation, external module, etc.).
 * 
 * @author Assaf Urieli
 *
 */
public class PretokenisedSequence extends TokenSequence {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PretokenisedSequence.class);
	private static final long serialVersionUID = 2675309892340757939L;

	public PretokenisedSequence(PretokenisedSequence sequenceToClone) {
		super(sequenceToClone);
		if (this.getText().length() > 0)
			this.textProvided = true;
	}

	public PretokenisedSequence(TalismaneSession talismaneSession) {
		this("", talismaneSession);
	}

	public PretokenisedSequence(String text, TalismaneSession talismaneSession) {
		super(new Sentence(text, talismaneSession), talismaneSession);
		if (text.length() > 0)
			this.textProvided = true;
	}

	@Override
	public Token addToken(int start, int end) {
		throw new TalismaneException("Cannot add tokens by index");

	}

	/**
	 * Adds a token to the current sequence, where the sequence is constructed
	 * from unit tokens, rather than from an existing sentence. Will
	 * automatically attempt to add the correct whitespace prior to this token.
	 */
	public Token addToken(String tokenText) {
		Token token = null;

		if (this.size() == 0) {
			// do nothing
		} else if (!textProvided) {
			// check if a space should be added before this token
			LinguisticRules rules = this.getTalismaneSession().getLinguisticRules();
			if (rules == null)
				throw new TalismaneException("Linguistic rules have not been set.");

			if (rules.shouldAddSpace(this, tokenText))
				this.addTokenInternal(" ");

		}
		token = this.addTokenInternal(tokenText);

		return token;
	}

	@Override
	public TokenSequence cloneTokenSequence() {
		PretokenisedSequence tokenSequence = new PretokenisedSequence(this);
		return tokenSequence;
	}
}
