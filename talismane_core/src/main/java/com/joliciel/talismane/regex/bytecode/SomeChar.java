///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2016 Safety Data -CFH
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
package com.joliciel.talismane.regex.bytecode;

/**
 * @author Lucas Satabin
 *
 */
public class SomeChar extends Instruction {

	public final char c;

	public SomeChar(char c) {
		super(OpCode.SOME_CHAR);
		this.c = c;
	}

	@Override
	public String toString() {
		return "char " + c;
	}

	@Override
	public SomeChar resolved(int base) {
		return this;
	}

	@Override
	public Instruction rebaseCaptures(int base) {
		return this;
	}

}