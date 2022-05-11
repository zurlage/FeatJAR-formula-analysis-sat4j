package org.spldev.analysis.sat4j;

import java.util.*;

import org.spldev.clauses.*;
import org.spldev.clauses.solutions.analysis.*;
import org.spldev.formula.*;
import org.spldev.formula.structure.atomic.*;
import org.spldev.util.data.*;

public class ConfigurationCompletor implements SolutionUpdater {
	private final ConfigurationGenerator generator;
	private final ModelRepresentation model;

	public ConfigurationCompletor(ModelRepresentation model, ConfigurationGenerator generator) {
		this.generator = generator;
		this.model = model;
	}

	@Override
	public LiteralList update(LiteralList partialSolution) {
		final CoreDeadAnalysis analysis = new CoreDeadAnalysis();
		for (int literal : partialSolution.getLiterals()) {
			analysis.getAssumptions().set(Math.abs(literal), literal > 0);
		}
		return partialSolution.addAll(model.get(analysis));
	}

	@Override
	public Optional<LiteralList> complete(LiteralList partialSolution) {
		final LiteralList result;
		if (partialSolution == null) {
			result = generator.get();
		} else {
			final Assignment assumptions = generator.getAssumptions();
			final List<Pair<Integer, Object>> oldAssumptions = assumptions.getAll();

			for (int literal : partialSolution.getLiterals()) {
				assumptions.set(Math.abs(literal), literal > 0);
			}
			generator.updateAssumptions();

			result = generator.get();

			assumptions.unsetAll();
			assumptions.setAll(oldAssumptions);
			generator.updateAssumptions();
		}
		return Optional.ofNullable(result);
	}

}
