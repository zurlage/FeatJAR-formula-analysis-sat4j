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
package de.featjar.formula.analysis.todo.mig.solver.visitor;

import de.featjar.formula.analysis.todo.mig.solver.ModalImplicationGraph;
import de.featjar.formula.analysis.todo.mig.solver.Vertex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.sat4j.core.VecInt;
import org.sat4j.specs.IteratorInt;

public class TransitiveTraverser extends ATraverser {

    public TransitiveTraverser(ModalImplicationGraph modalImplicationGraph) {
        super(modalImplicationGraph);
    }

    @Override
    public void traverse(int... curLiterals) {
        final HashMap<SortedIntegerList, VecInt> complexClauseMap = new HashMap<>();
        Arrays.fill(dfsMark, false);

        traverseStrong(complexClauseMap, curLiterals);
        mainLoop:
        while (true) {
            for (final Iterator<Entry<SortedIntegerList, VecInt>> entryIterator =
                 complexClauseMap.entrySet().iterator();
                 entryIterator.hasNext(); ) {
                final Entry<SortedIntegerList, VecInt> entry = entryIterator.next();
                final VecInt v = entry.getValue();
                if (v != null) {
                    for (final IteratorInt iterator = v.iterator(); iterator.hasNext(); ) {
                        final int literal = iterator.next();
                        if (currentConfiguration[Math.abs(literal) - 1] == 0) {
                            final int vertexIndex = ModalImplicationGraph.getVertexIndex(literal);
                            if (!dfsMark[vertexIndex]) {
                                dfsMark[vertexIndex] = true;
                                final Vertex vertex = modalImplicationGraph.getVertex(literal);
                                boolean changed = false;
                                final Visitor.VisitResult visitWeakResult = visitor.visitWeak(literal);
                                switch (visitWeakResult) {
                                    case Cancel:
                                        return;
                                    case Continue:
                                        changed |= addComplexClauses(complexClauseMap, vertex) > 0;
                                        break;
                                    case Select:
                                        changed |= attemptStrongSelect(literal, complexClauseMap);
                                        break;
                                    case Skip:
                                        break;
                                    default:
                                        throw new AssertionError(visitWeakResult);
                                }
                                changed |= processComplexClauses(complexClauseMap);
                                if (changed) {
                                    continue mainLoop;
                                }
                            }
                        }
                    }
                }
            }
            break;
        }
    }

    @Override
    public void traverseStrong(int... curLiterals) {
        traverseStrong(new HashMap<>(), curLiterals);
    }

    private void traverseStrong(final HashMap<SortedIntegerList, VecInt> complexClauseMap, int... curLiterals) {
        boolean changed = false;
        for (final int curLiteral : curLiterals) {
            changed |= attemptStrongSelect(curLiteral, complexClauseMap);
        }
        if (changed) {
            processComplexClauses(complexClauseMap);
        }
    }

    private boolean processComplexClauses(final HashMap<SortedIntegerList, VecInt> complexClauseMap) {
        boolean changedInLoop, changed = false;
        do {
            changedInLoop = false;
            final List<VecInt> unitClauses = new LinkedList<>();
            for (final Entry<SortedIntegerList, VecInt> entry : complexClauseMap.entrySet()) {
                final VecInt v = entry.getValue();
                if (v != null) {
                    for (int j = v.size() - 1; j >= 0; j--) {
                        final int literal = v.get(j);
                        final int value = currentConfiguration[Math.abs(literal) - 1];
                        if (value != 0) {
                            if (value == literal) {
                                entry.setValue(null);
                            } else {
                                v.delete(j);
                            }
                            changed = true;
                        }
                    }

                    if (v.size() == 1) {
                        entry.setValue(null);
                        unitClauses.add(v);
                    }
                }
            }

            for (final VecInt v : unitClauses) {
                changedInLoop |= attemptStrongSelect(v.get(0), complexClauseMap);
            }
            changed |= changedInLoop;
        } while (changedInLoop);
        return changed;
    }

    private boolean attemptStrongSelect(final int curLiteral, final HashMap<SortedIntegerList, VecInt> complexClauseMap) {
        final int modelIndex = Math.abs(curLiteral) - 1;
        if (currentConfiguration[modelIndex] == 0) {
            currentConfiguration[modelIndex] = curLiteral;

            final Visitor.VisitResult visitStrongResult = visitor.visitStrong(curLiteral);
            switch (visitStrongResult) {
                case Cancel:
                    // TODO
                    return false;
                case Skip:
                    return false;
                case Select:
                case Continue:
                    break;
                default:
                    throw new AssertionError(visitStrongResult);
            }
            final Vertex curVertex = modalImplicationGraph.getVertex(curLiteral);
            if (complexClauseMap != null) {
                addComplexClauses(complexClauseMap, curVertex);
            }
            for (final Vertex strongEdge : curVertex.getStrongEdges()) {
                attemptStrongSelect(strongEdge.getVar(), complexClauseMap);
            }
            if (complexClauseMap != null) {
                return false;
            }
            return true;
        }
        return false;
    }

    private int addComplexClauses(final HashMap<SortedIntegerList, VecInt> complexClauseMap, final Vertex vertex) {
        int added = 0;
        final List<SortedIntegerList> complexSortedIntegerLists = vertex.getComplexClauses();
        for (final SortedIntegerList sortedIntegerList : complexSortedIntegerLists) {
            if (!complexClauseMap.containsKey(sortedIntegerList)) {
                complexClauseMap.putIfAbsent(sortedIntegerList, new VecInt(Arrays.copyOf(sortedIntegerList.getIntegers(), sortedIntegerList.size())));
                added++;
            }
        }
        return added;
    }
}
