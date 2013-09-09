package br.ufmg.dcc.labsoft.java.jmove.view;

import gr.uom.java.jdeodorant.refactoring.manipulators.MoveMethodRefactoring;
import gr.uom.java.jdeodorant.refactoring.views.MyRefactoringWizard;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import br.ufmg.dcc.labsoft.java.jmove.principal.ChamaRefine;
import br.ufmg.dcc.labsoft.java.jmove.suggestion.Suggestion;
import br.ufmg.dcc.labsoft.java.jmove.utils.CandidateMap;
import br.ufmg.dcc.labsoft.java.jmove.utils.LogSystem;

public class JMoveView extends ViewPart {
	private TableViewer tableViewer;
	private IJavaProject selectedProject;
	private Action applyRefactoringAction;
	private Action identifyBadSmellsAction;
	private Action doubleClickAction;
	private Action saveResultsAction;
	private CandidateMap map;

	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(40, true));
		layout.addColumnData(new ColumnWeightData(20, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		column1.setText("Source Method");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		column2.setText("Target Class");
		column2.setResizable(true);
		column2.pack();

		tableViewer.setColumnProperties(new String[] { "type", "source",
				"target" });

		makeActions();
		hookDoubleClickAction();
		contributeToActionBars();
		getSite().getWorkbenchWindow().getSelectionService()
				.addSelectionListener(selectionListener);

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		tableViewer.getControl().setFocus();
	}

	private ISelectionListener selectionListener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart,
				ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Object element = structuredSelection.getFirstElement();
				IJavaProject javaProject = null;

				if (element instanceof IJavaProject) {
					javaProject = (IJavaProject) element;
					selectedProject = javaProject;
					identifyBadSmellsAction.setEnabled(true);
					saveResultsAction.setEnabled(false);
				}

			}
		}
	};

	private void hookDoubleClickAction() {
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(identifyBadSmellsAction);
		manager.add(applyRefactoringAction);
		manager.add(saveResultsAction);
	}

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {

			if (map != null) {
				Object[] cand = map.getCandidates();
				if (cand.length == 0) {// no recommendations
					MessageDialog.openInformation(getSite()
							.getWorkbenchWindow().getShell(),
							"Recommendations not found",
							"No recommendations were found for this system.");
					return new CandidateMap[0];
				} else
					return cand;
			} else {
				return new CandidateMap[0];
			}
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {

			Suggestion sug = (Suggestion) obj;

			switch (index) {
			case 0:
				return "Move Method";
			case 1:
				return sug.getMethodSignature();
			case 2:
				return sug.getClassName();
			default:
				return "";
			}

		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}

		public Image getImage(Object obj) {
			return null;
		}

	}

	private void makeActions() {
		identifyBadSmellsAction = new Action() {
			public void run() {
				// CompilationUnitCache.getInstance().clearCache();
				map = getTable();
				tableViewer.setContentProvider(new ViewContentProvider());
				applyRefactoringAction.setEnabled(true);
				saveResultsAction.setEnabled(true);

			}
		};
		identifyBadSmellsAction
				.setToolTipText("Identify Move Methods Opportunities");
		identifyBadSmellsAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		identifyBadSmellsAction.setEnabled(false);

		saveResultsAction = new Action() {
			public void run() {
				saveResults();
			}
		};
		saveResultsAction.setToolTipText("Save Results");
		saveResultsAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveResultsAction.setEnabled(false);

		// APLICANDO REFATORAÇÂO
		applyRefactoringAction = new Action() {
			public void run() {
		
				IStructuredSelection selection = (IStructuredSelection) tableViewer
						.getSelection();
				Suggestion sug = (Suggestion) selection.getFirstElement();

				IFile sourceFile = sug.getSourceIFile();
				IFile targetFile = sug.getTargetIFile();

				CompilationUnit sourceCompilationUnit = sug
						.getSourceCompilationUnit();

				CompilationUnit targetCompilationUnit = sug
						.getTargetClassCompilationUnit();

				Refactoring refactoring = null;

				refactoring = new MoveMethodRefactoring(sourceCompilationUnit,
						targetCompilationUnit,
						sug.recoverSourceClassTypeDeclaration(),
						sug.recovergetTargetClassTypeDeclaration(),
						sug.getSourceMethodDeclaration(),
						new HashMap<MethodInvocation, MethodDeclaration>(),
						false, sug.getMethodName());

				MyRefactoringWizard wizard = new MyRefactoringWizard(
						refactoring, applyRefactoringAction);

				RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
						wizard);
				try {
					String titleForFailedChecks = ""; //$NON-NLS-1$ 
					op.run(getSite().getShell(), titleForFailedChecks);
				} catch (InterruptedException e) {
					e.printStackTrace();
					LogSystem.write(e);
				}
				try {
					IJavaElement targetJavaElement = JavaCore
							.create(targetFile);
					JavaUI.openInEditor(targetJavaElement);
					IJavaElement sourceJavaElement = JavaCore
							.create(sourceFile);
					JavaUI.openInEditor(sourceJavaElement);
				} catch (PartInitException e) {
					e.printStackTrace();
					LogSystem.write(e);
				} catch (JavaModelException e) {
					e.printStackTrace();
					LogSystem.write(e);
				}

			}
		};
		applyRefactoringAction.setToolTipText("Apply Refactoring");
		applyRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		applyRefactoringAction.setEnabled(false);

		doubleClickAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) tableViewer
						.getSelection();
				Suggestion sug = (Suggestion) selection.getFirstElement();

				IFile sourceFile = sug.getSourceIFile();
				IFile targetFile = sug.getTargetIFile();
				try {
					IJavaElement targetJavaElement = JavaCore
							.create(targetFile);
					JavaUI.openInEditor(targetJavaElement);
					IJavaElement sourceJavaElement = JavaCore
							.create(sourceFile);
					ITextEditor sourceEditor = (ITextEditor) JavaUI
							.openInEditor(sourceJavaElement);
					List<Position> positions = sug.getPositions();
					AnnotationModel annotationModel = (AnnotationModel) sourceEditor
							.getDocumentProvider().getAnnotationModel(
									sourceEditor.getEditorInput());
					Iterator<Annotation> annotationIterator = annotationModel
							.getAnnotationIterator();
					while (annotationIterator.hasNext()) {
						Annotation currentAnnotation = annotationIterator
								.next();
						if (currentAnnotation.getType().equals(
								SliceAnnotation.EXTRACTION)) {
							annotationModel.removeAnnotation(currentAnnotation);
						}
					}
					for (Position position : positions) {
						SliceAnnotation annotation = new SliceAnnotation(
								SliceAnnotation.EXTRACTION,
								sug.getAnnotationText());
						annotationModel.addAnnotation(annotation, position);
					}
					Position firstPosition = positions.get(0);
					Position lastPosition = positions.get(positions.size() - 1);
					int offset = firstPosition.getOffset();
					int length = lastPosition.getOffset()
							+ lastPosition.getLength()
							- firstPosition.getOffset();
					sourceEditor.setHighlightRange(offset, length, true);
				} catch (PartInitException e) {
					e.printStackTrace();
					LogSystem.write(e);
				} catch (JavaModelException e) {
					e.printStackTrace();
					LogSystem.write(e);
				}

			}
		};
	}

	private void saveResults() {
		FileDialog fd = new FileDialog(getSite().getWorkbenchWindow()
				.getShell(), SWT.SAVE);
		fd.setText("Save Results");
		String[] filterExt = { "*.txt" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			try {
				BufferedWriter out = new BufferedWriter(
						new FileWriter(selected));
				Table table = tableViewer.getTable();
				TableColumn[] columns = table.getColumns();
				for (int i = 0; i < columns.length; i++) {
					if (i == columns.length - 1)
						out.write(columns[i].getText());
					else
						out.write(columns[i].getText() + "\t");
				}
				out.newLine();
				for (int i = 0; i < table.getItemCount(); i++) {
					TableItem tableItem = table.getItem(i);
					for (int j = 0; j < table.getColumnCount(); j++) {
						if (j == table.getColumnCount() - 1)
							out.write(tableItem.getText(j));
						else
							out.write(tableItem.getText(j) + "\t");
					}
					out.newLine();
				}
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				LogSystem.write(e);
			}
		}
	}

	public CandidateMap getTable() {

		ChamaRefine refine = new ChamaRefine();// chamar com o nome do projeto
												// #selectedProject

		return refine.execute(selectedProject);
	}

}
