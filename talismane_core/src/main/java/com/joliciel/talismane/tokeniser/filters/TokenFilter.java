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
package com.joliciel.talismane.tokeniser.filters;

import java.util.List;
import java.util.Map;

/**
 * A filter that takes raw text, and finds tokens in the text (which are
 * indicated by placeholders).<br/>
 * Note that, in addition to indicating tokens, it is possible to stipulate that
 * a sentence boundary will never be detected inside a placeholder.<br/>
 * Note that the tokeniser might still join the "atomic tokens" defined by the
 * token filter into larger tokens.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokenFilter {
	/**
	 * Analyse the sentence, and provide placeholders for any tokens that will
	 * have to be formed.
	 */
	public List<TokenPlaceholder> apply(String text);

	/**
	 * Load the token filter's state using information extracted from a
	 * descriptor.
	 * 
	 * @param parameters
	 *            a series of name/value parameters
	 * @param tabs
	 *            a list of unnamed parameters, whose placement determines their
	 *            meaning
	 * @throws TokenFilterLoadException
	 *             if any loading error occurs related to the parameters or tabs
	 */
	public void load(Map<String, String> parameters, List<String> tabs) throws TokenFilterLoadException;

	/**
	 * Returns true if this TokenFilter should be excluded from the list of
	 * TokenFilters for the current configuration. This will typically be set
	 * during the load method, based on context specific considerations.
	 */
	public boolean isExcluded();
}
