/*
 * Copyright (C) 2022 Sebastian Krieter
 *
 * This file is part of formula-analysis-sat4j.
 *
 * formula-analysis-sat4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula-analysis-sat4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula-analysis-sat4j. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula-analysis-sat4j> for further information.
 */
package de.featjar.formula.analysis.sat4j.twise;

import de.featjar.formula.analysis.bool.BooleanAssignmentList;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a presence condition as an expression.
 *
 * @author Sebastian Krieter
 */
public class PresenceCondition extends BooleanAssignmentList {


    private final transient TreeSet<Integer> groups = new TreeSet<>();

    public PresenceCondition() {
    }

    public PresenceCondition(BooleanAssignmentList otherBooleanAssignmentList) {
        super(otherBooleanAssignmentList);
    }

    public PresenceCondition(Collection<? extends SortedIntegerList> c) {
        super(c);
    }

    public PresenceCondition(int size) {
        super(size);
    }

    public void addGroup(int group) {
        groups.add(group);
    }

    public Set<Integer> getGroups() {
        return groups;
    }

    @Override
    public String toString() {
        return "Expression [" + super.toString() + "]";
    }
}
