package de.featjar.formula.analysis.sat4j.solver;

import de.featjar.formula.analysis.bool.BooleanSolution;

import java.util.*;

public interface SolutionHistory extends Iterable<BooleanSolution> {
    List<BooleanSolution> getSolutionHistory();
    Optional<BooleanSolution> getLastSolution();
    void setLastSolution(BooleanSolution solution);
    void addNewSolution(BooleanSolution solution);
    void clear();

    @Override
    default Iterator<BooleanSolution> iterator() {
        return getSolutionHistory().iterator();
    }

    class RememberLast implements SolutionHistory {
        protected BooleanSolution lastSolution;

        @Override
        public List<BooleanSolution> getSolutionHistory() {
            return List.of(lastSolution);
        }

        @Override
        public Optional<BooleanSolution> getLastSolution() {
            return Optional.ofNullable(lastSolution);
        }

        @Override
        public void setLastSolution(BooleanSolution solution) {
            this.lastSolution = solution;
        }

        @Override
        public void addNewSolution(BooleanSolution solution) {
            setLastSolution(solution);
        }

        @Override
        public void clear() {
            lastSolution = null;
        }
    }

    class RememberUpTo extends RememberLast {
        protected int limit;
        protected final LinkedList<BooleanSolution> solutionHistory = new LinkedList<>();

        public RememberUpTo(int limit) {
            if (limit < 1)
                throw new IllegalArgumentException();
            this.limit = limit;
        }

        @Override
        public List<BooleanSolution> getSolutionHistory() {
            return solutionHistory;
        }

        @Override
        public void addNewSolution(BooleanSolution solution) {
            super.addNewSolution(solution);
            solutionHistory.addFirst(solution);
            if (solutionHistory.size() > limit) {
                solutionHistory.removeLast();
            }
        }

        @Override
        public void clear() {
            super.clear();
            solutionHistory.clear();
        }
    }
}
