package br.ufmg.dcc.labsoft.java.jmove.principal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach;
import br.ufmg.dcc.labsoft.java.jmove.ast.DeepDependencyVisitor;
import br.ufmg.dcc.labsoft.java.jmove.basic.CoefficientsResolution.CoefficientStrategy;
import br.ufmg.dcc.labsoft.java.jmove.methods.AllMethods;
import br.ufmg.dcc.labsoft.java.jmove.methods.Clazz;
import br.ufmg.dcc.labsoft.java.jmove.util.DCLUtil;
import br.ufmg.dcc.labsoft.java.jmove.utils.CandidateMap;
import br.ufmg.dcc.labsoft.java.jmove.utils.InternalClass;
import br.ufmg.dcc.labsoft.java.jmove.utils.LogSystem;

public class ChamaRefine {

	CandidateMap map;
	IProject project;
	IJavaProject javaProject;
	List<DeepDependencyVisitor> allDeepDependency;
	int numberOfClass = 0;
	AllMethods allMethods;
	String activeProjectName;

	public CandidateMap execute(IJavaProject iProject) {
		try {

			allDeepDependency = new ArrayList<DeepDependencyVisitor>();

			activeProjectName = iProject.getElementName();

			project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(activeProjectName);
			javaProject = JavaCore.create(project);

			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					try {
						monitor.beginTask(
								"Parsing selected Java Project (1/4)",
								(DCLUtil.getClassNames(project)).size());

						for (String className : DCLUtil.getClassNames(project)) {
							if (className == null) {
								continue;
							}

							InternalClass.getInstance().putNewInternalClass(
									className);

							IFile resource = DCLUtil.getFileFromClassName(
									javaProject, className);
							ICompilationUnit unit = ((ICompilationUnit) JavaCore
									.create((IFile) resource));

							DeepDependencyVisitor deepDependency = new DeepDependencyVisitor(
									unit);
							allDeepDependency.add(deepDependency);
							numberOfClass++;

							Clazz.getInstance().insertMapping(unit);
							monitor.worked(1);

							if (monitor != null && monitor.isCanceled()) {
								if (monitor != null)
									monitor.done();
								throw new OperationCanceledException();
							}
						}
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						LogSystem.write(e);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						LogSystem.write(e);
					}
				}
			});

			wb = PlatformUI.getWorkbench();
			ps = wb.getProgressService();
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					try {
						allMethods = new AllMethods(allDeepDependency, monitor);
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						LogSystem.write(e);
					}
				}
			});

			wb = PlatformUI.getWorkbench();
			ps = wb.getProgressService();
			ps.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					CalculateMediaApproach mediaApproach = new CalculateMediaApproach(
							allMethods, activeProjectName, numberOfClass,
							monitor);
					allMethods = null;
					map = mediaApproach
							.calculate(CoefficientStrategy.SokalSneath2);

				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			LogSystem.write(e);
		}

		return map;

	}
}
