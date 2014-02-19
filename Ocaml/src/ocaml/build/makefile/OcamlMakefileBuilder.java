package ocaml.build.makefile;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.build.ProblemMarkers;
import ocaml.exec.CommandRunner;
import ocaml.exec.ExecHelper;
import ocaml.exec.IExecEvents;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * Builder associated with projects of the "ocamlnatureMakefile" nature. It is used to compile this kind of
 * projects, clean them, and generate their documentation (all by using the user-specified makefile targets)
 */
public class OcamlMakefileBuilder extends IncrementalProjectBuilder {

	public static final String ID = "Ocaml.ocamlMakefileBuilder";

	public OcamlMakefileBuilder() {
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		build(IncrementalProjectBuilder.CLEAN_BUILD, null, monitor);
	}

	/** For the "clean project" action : only clean, don't recompile */
	public void clean(IProject project, IProgressMonitor monitor) {
		try {
			/* We use a variable here because getProject() is final in the super-class */
			this.project = project;
			this.build(IncrementalProjectBuilder.CLEAN_BUILD, null, monitor);
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		} finally {
			this.project = null;
		}
	}

	/** Is a build in progress? */
	private static boolean building = false;

	/** To make sure only one build can be active at any time */
	private static final Object buildMutex = new Object();

	private IProject project = null;

	
	@Override
	protected IProject[] build(final int kind, @SuppressWarnings("rawtypes") final Map args, final IProgressMonitor monitor)
			throws CoreException {

		// don't start two builds simultaneously
		synchronized (buildMutex) {
			if (building) {
				// System.out.println("already building: aborting");
				return null;
			} else
				building = true;
		}

		final IProgressMonitor buildMonitor;
		if (monitor == null)
			buildMonitor = new NullProgressMonitor();
		else
			buildMonitor = monitor;

		try {
			if (kind == CLEAN_BUILD)
				buildMonitor.beginTask("Cleaning Project", IProgressMonitor.UNKNOWN);
			else
				buildMonitor.beginTask("Making Project", IProgressMonitor.UNKNOWN);

			final IProject project;
			if (this.project == null)
				project = this.getProject();
			else
				project = this.project;

			if (kind == IncrementalProjectBuilder.AUTO_BUILD)
				return null;

			MakefileProperties makefileProperties = new MakefileProperties(project);
			
			String makeCmd = "";
			switch (makefileProperties.getVariant()) {
			case GNU_MAKE:
				makeCmd = OcamlPlugin.getMakeFullPath();
				break;
			case OMAKE:
				makeCmd = OcamlPlugin.getOMakeFullPath();
				break;
			}
			makeCmd = makeCmd.trim();
			if (makeCmd.trim().equals("")) {
				OcamlPlugin.logError("The " + makefileProperties.getVariant().getName()
						+ " command couldn't be found. Please configure its path in the preferences.");
				return null;
			}

			String path = project.getLocation().toOSString();

			ArrayList<String> commandLine = new ArrayList<String>();
			commandLine.add(makeCmd);
			switch (makefileProperties.getVariant()) {
			case OMAKE:
				commandLine.add("--no--progress");
				commandLine.add("-w");
				break;
			case GNU_MAKE:
				commandLine.add("-C");
				commandLine.add(path);
				break;
			}
			
			for (String option : makefileProperties.getOptions())
				commandLine.add(option);

			String[] targets = null;
			if (kind == CLEAN_BUILD)
				targets = makefileProperties.getCleanTargets();
			else
				targets = makefileProperties.getTargets();

			for (String target: targets)
				commandLine.add(target);

			String[] strCommandLine = commandLine.toArray(new String[commandLine.size()]);

			final StringBuilder output = new StringBuilder();

			IExecEvents events = new IExecEvents() {

				public void processNewInput(final String input) {
					output.append(input);
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							OcamlCompilerOutput outputView = OcamlCompilerOutput.get();
							if (outputView != null)
								outputView.append(input);

						}
					});
				}

				// not used, because the error output is merged with the standard output
				public void processNewError(String error) {
				}

				public void processEnded(int exitValue) {
					if (kind == CLEAN_BUILD)
						cleanFinished(project);
					else
						makefileFinished(output.toString(), project);
				}
			};

			// clean the output from the last compilation
			/*
			 * Display.getDefault().syncExec(new Runnable() { public void run() { OcamlCompilerOutput output =
			 * OcamlCompilerOutput.get(); if (output != null) output.clear(); } });
			 */

			File dir = project.getLocation().toFile();
			ExecHelper execHelper = null;
			try {
				// use OS-inherited environment for the make process
				execHelper = ExecHelper.execMerge(events, strCommandLine, System.getenv(), dir);
			} catch (Exception e) {
				OcamlPlugin.logError("ocaml plugin error", e);
				return null;
			}

			/*
			 * Check at regular intervals whether the user canceled the build. When that happens, we kill the
			 * "make" process.
			 */
			while (execHelper.isRunning()) {
				if (buildMonitor.isCanceled())
					execHelper.kill();
				try {
					buildMonitor.worked(1);
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}

			execHelper.join();

			return null;
		} finally {
			buildMonitor.worked(1);
			buildMonitor.done();
			building = false;
		}
	}

	// private String findMakePaths(IProject project) {
	// OcamlMakefilePaths opaths = new OcamlMakefilePaths(project);
	// StringBuilder strBuilder = new StringBuilder();
	// for(String p : opaths.getPaths())
	// strBuilder.append(p + File.pathSeparatorChar);
	//
	// // remove the last separator
	// strBuilder.setLength(strBuilder.length() - 1);
	// return strBuilder.toString();
	// }

	protected void cleanFinished(final IProject project) {
		/*
		 * This is executed asynchronously because the refresh operation is waiting for the completion of the
		 * build() method to run.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					project.refreshLocal(IProject.DEPTH_INFINITE, null);
					project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});

	}

	protected void makefileFinished(final String output, final IProject project) {

		// Execute a background job to decorate files with markers
		Job job = new Job("Decorating Project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					/*
					 * Refresh the project to see modifications. Eclipse waits for the build Job to finish
					 * before executing this refresh.
					 */
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							try {
								project.refreshLocal(IProject.DEPTH_INFINITE, null);
							} catch (CoreException e1) {
								OcamlPlugin.logError("ocaml plugin error", e1);
							}
						}
					});

					IFile[] files = Misc.getProjectFiles(project);

					monitor.beginTask("Decorating Project", files.length + 3);

					/*
					 * Delete all markers on the project (since we rebuilt it). This can be problematic with
					 * warning markers, that will disappear at the next rebuild (since files with only
					 * warnings won't be recompiled). The warning marker will only reappear next time the file
					 * in which it appears is modified.
					 */
					try {
						project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}

					monitor.worked(1);

					/*
					 * Parse the compiler output to find error and warning messages.
					 */
					ProblemMarkers problemMarkers = new ProblemMarkers(project);
					problemMarkers.makeMarkers2(output.toString());

					monitor.worked(1);

					// Remove the "error" and "warning" property on each project file
					for (IFile f : files) {
						Misc.setFileProperty(f, OcamlBuilder.COMPILATION_ERRORS, null);
						Misc.setFileProperty(f, OcamlBuilder.COMPILATION_WARNINGS, null);
					}

					/*
					 * Put a "warning" property on files that generated at least a warning but not any error
					 */
					for (IFile f : problemMarkers.getFilesWithWarnings())
						Misc.setFileProperty(f, OcamlBuilder.COMPILATION_WARNINGS, "true");

					/* Put an "error" property on files that generated at least an error */
					for (IFile f : problemMarkers.getFilesWithErrors())
						Misc.setFileProperty(f, OcamlBuilder.COMPILATION_ERRORS, "true");

					Misc.updateDecoratorManager();

					monitor.worked(1);

					/*
					 * Test each file to determine whether it is an executable, using the "file" system
					 * command. Depending on the result, mark them as byte-code or native executables.
					 */
					if (OcamlPlugin.runningOnLinuxCompatibleSystem()) {
						String[] command = new String[2];

						for (IFile file : files) {
							monitor.worked(1);

							if (monitor.isCanceled())
								break;

							String name = file.getName();
							if (name.equalsIgnoreCase("makefile") || name.equalsIgnoreCase("OCamlMakefile")
									|| name.equalsIgnoreCase("OMakefile")
									|| name.equalsIgnoreCase("OMakeroot"))
								continue;

							// Skip upper-case filenames (README, INSTALL)
							if (name.equals(name.toUpperCase()))
								continue;

							String extension = file.getFileExtension();
							if (extension == null || extension.matches("exe|out|byte|opt")) {
								command[0] = "file";
								command[1] = file.getLocation().toOSString();
								CommandRunner commandRunner = new CommandRunner(command, "/");
								String result = commandRunner.getStdout();
								if (result.contains("ocamlrun script"))
									Misc.setShareableProperty(file, OcamlBuilder.COMPIL_MODE,
											OcamlBuilder.BYTE_CODE);
								else if (result.contains("executable"))
									Misc.setShareableProperty(file, OcamlBuilder.COMPIL_MODE, OcamlBuilder.NATIVE);
							}
						}
					}
					// on Windows, find "*.exe" files
					else {
						for (IFile file : files) {
							monitor.worked(1);

							String extension = file.getFileExtension();
							if (extension != null && extension.equals("exe")) {
								Misc.setShareableProperty(file, OcamlBuilder.COMPIL_MODE, OcamlBuilder.NATIVE);
							}
						}
					}

				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error (file decorator)", e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		job.setPriority(Job.DECORATE);
		job.schedule(500);

		/*
		 * Do not join on this job, since it refreshes the workspace, and this operation waits for the build
		 * to finish first.
		 */
	}
}
