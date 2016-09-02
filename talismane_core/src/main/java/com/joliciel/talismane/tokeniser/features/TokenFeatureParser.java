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
package com.joliciel.talismane.tokeniser.features;

import java.util.List;

import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.features.Feature;
import com.joliciel.talismane.machineLearning.features.FeatureClassContainer;
import com.joliciel.talismane.machineLearning.features.FunctionDescriptor;
import com.joliciel.talismane.tokeniser.patterns.TokenPattern;

public interface TokenFeatureParser {

	public void addFeatureClasses(FeatureClassContainer container);

	public List<FunctionDescriptor> getModifiedDescriptors(FunctionDescriptor functionDescriptor);

	public List<TokenPattern> getPatternList();

	public void setPatternList(List<TokenPattern> patternList);

	public void injectDependencies(@SuppressWarnings("rawtypes") Feature feature);

	public TalismaneSession getTalismaneSession();
}