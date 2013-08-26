package br.ufmg.dcc.labsoft.java.jmove.approach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;

import br.ufmg.dcc.labsoft.java.jmove.basic.AllEntitiesMapping;
import br.ufmg.dcc.labsoft.java.jmove.basic.CoefficientsResolution;
import br.ufmg.dcc.labsoft.java.jmove.basic.CoefficientsResolution.CoefficientStrategy;
import br.ufmg.dcc.labsoft.java.jmove.basic.Parameters;
import br.ufmg.dcc.labsoft.java.jmove.methods.AllMethods;
import br.ufmg.dcc.labsoft.java.jmove.methods.MethodJMove;
import br.ufmg.dcc.labsoft.java.jmove.methods.StatisticsMethod2Method;
import br.ufmg.dcc.labsoft.java.jmove.utils.CandidateMap;

public class CalculateMediaApproach {

	final int indexCORRETA = 0;
	final int indexSUGESTAO = 1;
	final int indexERRADO = 3;
	CandidateMap candidateMap;
	boolean needCalculateAll;

	private class ClassAtributes { // classe interna usada somente dentro da
									// classe
		int classID;
		int numberOfMethods;
		double similarityIndice;

		public ClassAtributes(int classID) {
			super();
			this.classID = classID;
			this.numberOfMethods = 0;
			this.similarityIndice = 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ClassAtributes) {
				// TODO Auto-generated method stub
				ClassAtributes other = (ClassAtributes) obj;
				if (this.classID != other.classID) {
					return false;
				}
				return true;
			}
			return false;
		}

	}

	// private Map<Pair<MethodRefine, MethodRefine>, Parameters> allParameters;
	private AllMethods allMethods;

	private IProgressMonitor monitor;
	private StatisticsMethod2Method stats;


	public CalculateMediaApproach(AllMethods allMethods,
			String activeProjectName, int numberOfClass,
			IProgressMonitor monitor) {
		// TODO Auto-generated constructor stub
		this.allMethods = allMethods;
		this.monitor = monitor;
		stats = new StatisticsMethod2Method(allMethods.getAllMethodsList());

	}

	public CandidateMap calculate(CoefficientStrategy strategy) {

		monitor.beginTask(
				"Calculating methods similarity for selected Java Project (3/4)",
				(allMethods.getAllMethodsList().size()));

		candidateMap = new CandidateMap();

		int contador[] = { 0, 0, 0, 0, 0 };

		CoefficientsResolution resolution = new CoefficientsResolution();

		List<ClassAtributes> allClassSimilarity = new ArrayList<ClassAtributes>();


		for (int i = 0; i < allMethods.getAllMethodsList().size(); i++) {

			monitor.worked(1);
			MethodJMove sourceMethod = allMethods.getAllMethodsList().get(i);

			// #### tira metodos pequenos
			if (sourceMethod.getMethodsDependencies().size() < 4) {
				continue;

			}


			// #########begin conta somente aqueles que algum move é possivel

			int source = sourceMethod.getNameID();
			if (!allMethods.getMoveIspossible().contains(source)) {
				continue;

			}

			// ########end;

			allClassSimilarity.clear();

			for (int j = 0; j < allMethods.getAllMethodsList().size(); j++) {

				if (i != j) {

					MethodJMove targetMethod = allMethods.getAllMethodsList()
							.get(j);
					int tagertClassID = targetMethod.getSourceClassID();

					ClassAtributes atributes = new ClassAtributes(tagertClassID);

					if (allClassSimilarity.contains(atributes)) {
						int index = allClassSimilarity.indexOf(atributes);
						atributes = allClassSimilarity.get(index);

					} else {

						allClassSimilarity.add(atributes);

					}

					atributes.numberOfMethods++;

					Parameters parameters = stats.calculateParameters(
							sourceMethod, targetMethod);

					atributes.similarityIndice += resolution.calculate(
							parameters, strategy);

				}

			}
			for (ClassAtributes classAtributes : allClassSimilarity) {
				classAtributes.similarityIndice /= classAtributes.numberOfMethods;
			}

			Collections.sort(allClassSimilarity, new Comparator<Object>() {
				public int compare(Object o1, Object o2) {
					ClassAtributes c1 = (ClassAtributes) o1;
					ClassAtributes c2 = (ClassAtributes) o2;
					return Double.compare(c2.similarityIndice,
							c1.similarityIndice);
				}
			});

			normalize(allClassSimilarity);
			blindAnalisys(allClassSimilarity, sourceMethod, contador);
			if (monitor != null && monitor.isCanceled()) {
				if (monitor != null)
					monitor.done();
				throw new OperationCanceledException();
			}
		}

		return candidateMap;

	}

	private void normalize(List<ClassAtributes> allClassSimilarity) {
		// TODO Auto-generated method stub

		double bigger = allClassSimilarity.get(0).similarityIndice;
		double minor = allClassSimilarity.get(0).similarityIndice;

		for (int i = 0; i < allClassSimilarity.size(); i++) {
			if (allClassSimilarity.get(i).similarityIndice > bigger) {
				bigger = allClassSimilarity.get(i).similarityIndice;
			} else if (allClassSimilarity.get(i).similarityIndice < minor) {
				minor = allClassSimilarity.get(i).similarityIndice;
			}
		}

		bigger -= minor;

		for (int i = 0; i < allClassSimilarity.size(); i++) {
			allClassSimilarity.get(i).similarityIndice -= minor;
			allClassSimilarity.get(i).similarityIndice /= bigger;
		}

	}
	

	private void blindAnalisys(List<ClassAtributes> allClassSimilarity,
			MethodJMove sourceMethod, int[] contador) {
		// TODO Auto-generated method stub

		ClassAtributes classOriginal = new ClassAtributes(
				sourceMethod.getSourceClassID());

		IMethod iMethod = allMethods.getIMethod(sourceMethod);

		int indexOf = allClassSimilarity.indexOf(classOriginal);

		// considera as três primeirsas posições no ranking
		final int POSICAOMAXIMA = 2;
		final double VALORALTO = 0.75;

		if (indexOf > POSICAOMAXIMA) {
			contador[indexERRADO]++;

			for (int i = 0; i < indexOf; i++) {
				ClassAtributes classAtributes = allClassSimilarity.get(i);
				String candidate = AllEntitiesMapping.getInstance().getByID(
						classAtributes.classID);
				candidateMap.putCandidateOnList(iMethod, candidate);
			}

		} else {
			contador[indexCORRETA]++;

			for (int i = 0; i < indexOf; i++) {
				ClassAtributes classAtributes = allClassSimilarity.get(i);
				ClassAtributes classAtributesSource = allClassSimilarity
						.get(indexOf);
				if (classAtributesSource.similarityIndice < VALORALTO) {
					String candidate = AllEntitiesMapping.getInstance()
							.getByID(classAtributes.classID);
					candidateMap.putCandidateOnList(iMethod, candidate);
				}
			}

		}


	}

	public void calculateForAllStrategies() {

		needCalculateAll = true;

		for (CoefficientStrategy strategy : CoefficientsResolution
				.AllCoefficientStrategy()) {
			calculate(strategy);
		}

	}
}
