/* -----------------------------------------------------------------------------
 * Formula-Analysis-Sat4J Lib - Library to analyze propositional formulas with Sat4J.
 * Copyright (C) 2021-2022  Sebastian Krieter
 * 
 * This file is part of Formula-Analysis-Sat4J Lib.
 * 
 * Formula-Analysis-Sat4J Lib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * Formula-Analysis-Sat4J Lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Formula-Analysis-Sat4J Lib.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * See <https://github.com/skrieter/formula-analysis-sat4j> for further information.
 * -----------------------------------------------------------------------------
 */
package org.spldev.analysis.sat4j.twise;

/**
 * Holds statistics regarding validity of configurations within a configuration
 * sample.
 *
 * @author Sebastian Krieter
 */
public class ValidityStatistic {

	protected final boolean[] configValidities;
	protected int numberOfValidConfigurations;

	public ValidityStatistic(int sampleSize) {
		configValidities = new boolean[sampleSize];
	}

	public void setConfigValidity(int index, boolean valid) {
		configValidities[index] = valid;
		if (valid) {
			numberOfValidConfigurations++;
		}
	}

	public boolean[] getConfigValidities() {
		return configValidities;
	}

	public int getNumberOfConfigurations() {
		return configValidities.length;
	}

	public int getNumberOfValidConfigurations() {
		return numberOfValidConfigurations;
	}

	public int getNumberOfInvalidConfigurations() {
		return configValidities.length - numberOfValidConfigurations;
	}

	public double getValidInvalidRatio() {
		if (configValidities.length != 0) {
			return ((double) numberOfValidConfigurations / (double) configValidities.length);
		} else {
			return 1.0;
		}
	}

}
