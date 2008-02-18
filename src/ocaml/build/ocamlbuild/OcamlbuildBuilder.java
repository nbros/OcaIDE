package ocaml.build.ocamlbuild;

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
import ocaml.util.OcamlPaths;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

public class OcamlbuildBuilder extends IncrementalProjectBuilder {

	public static final String ID = "Ocaml.ocamlbuildBuilder";

	/** Is a build in progress? */
	private boolean building = false;

	public OcamlbuildBuilder() {
	}

	/**
	 * Build the ocamlbuild command line which will compile the given project
	 * with the flags and paths the user defined in the project properties page.
	 * 
	 * @param project
	 *            the project to build
	 * @param noTargets
	 *            do not add targets to the build command line
	 */
	public static ArrayList<String> buildCommandLine(IProject project,
			boolean noTargets) {
		OcamlbuildFlags ocamlbuildFlags = new OcamlbuildFlags(project);
		ocamlbuildFlags.load();

		// String path = project.getLocation().toOSString();

		ArrayList<String> commandLine = new ArrayList<String>();

		String ocamlbuild = OcamlPlugin.getOcamlbuildFullPath().trim();
		if ("".equals(ocamlbuild)) {
			OcamlPlugin.logError("ocamlbuild path is not configured");
			return null;
		}

		commandLine.add(ocamlbuild);
		commandLine.add("-classic-display");
		commandLine.add("-no-log");

		OcamlPaths ocamlPaths = new OcamlPaths(project);
		String[] paths = ocamlPaths.getPaths();
		if (paths.length > 0) {
			for (String path : paths) {
				path = path.trim();

				File file = new File(path);
				// ocamlbuild doesn't accept absolute paths (only relative to the project root)
				if (!".".equals(path) && !file.isAbsolute()) {
					commandLine.add("-I");
					commandLine.add(path);
				}
			}
		}

		commandLine.add("-tags");
		commandLine.add("dtypes"); // TODO user preference?
		// commandLine.add("-log");
		// commandLine.add("_build" + File.separator + "_log");
		String libs = ocamlbuildFlags.getLibs();
		if (!"".equals(libs)) {
			commandLine.add("-libs");
			commandLine.add(libs);
		}
		String cflags = ocamlbuildFlags.getCFlags();
		if (!"".equals(cflags)) {
			commandLine.add("-cflags");
			commandLine.add(cflags);
		}
		String lflags = ocamlbuildFlags.getLFlags();
		if (!"".equals(lflags)) {
			commandLine.add("-lflags");
			commandLine.add(lflags);
		}

		if (!noTargets) {
			String[] targets = ocamlbuildFlags.getTargetsAsList();
			for (String target : targets)
				commandLine.add(target);
		}

		return commandLine;
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {

		// don't start two builds simultaneously
		if (building)
			return null;

		IResourceDelta delta = getDelta(getProject());
		if (delta != null && !containsChanges(delta))
			return null;

		final IProgressMonitor buildMonitor;
		if (monitor == null)
			buildMonitor = new NullProgressMonitor();
		else
			buildMonitor = monitor;

		try {
			if (kind == CLEAN_BUILD) {
				OcamlPlugin
						.logError("CLEAN_BUILD kind in build()? Shouldn't happen.");
				return null;
			}

			building = true;

			buildMonitor
					.beginTask("Building Project", IProgressMonitor.UNKNOWN);

			final IProject project = this.getProject();

			ArrayList<String> commandLine = buildCommandLine(project, false);
			if (commandLine == null)
				return null;

			String[] strCommandLine = commandLine
					.toArray(new String[commandLine.size()]);

			final StringBuilder output = new StringBuilder();

			IExecEvents events = new IExecEvents() {

				public void processNewInput(final String input) {
					// System.out.println(input);
					output.append(input);
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							OcamlCompilerOutput outputView = OcamlCompilerOutput
									.get();
							if (outputView != null)
								outputView.append(input);
						}
					});
				}

				// not used, because the error output is merged with the
				// standard output
				public void processNewError(String error) {
				}

				public void processEnded(int exitValue) {
					buildFinished(output.toString(), project);
				}

			};

			// clean the output from the last compilation
			/*Display.getDefault().syncExec(new Runnable() {
				public void run() {
					OcamlCompilerOutput output = OcamlCompilerOutput.get();
					if (output != null)
						output.clear();
				}
			});*/

			File dir = project.getLocation().toFile();

			ExecHelper execHelper = null;
			try {
				execHelper = ExecHelper.execMerge(events, strCommandLine, null,
						dir);
			} catch (Exception e) {
				OcamlPlugin.logError("ocaml plugin error", e);
				return null;
			}

			/*
			 * Check at regular intervals whether the user canceled the build.
			 * When that happens, we kill the "ocamlbuild" process.
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

		/*
		 * if (kind == IncrementalProjectBuilder.FULL_BUILD) {
		 * fullBuild(monitor); } else { IResourceDelta delta =
		 * getDelta(getProject()); if (delta == null) { fullBuild(monitor); }
		 * else { incrementalBuild(delta, monitor); } }
		 */
	}

	private final Object signal = new Object();
	private boolean refreshed = true;

	private void buildFinished(final String output, final IProject project) {
		refreshed = false;

		/*
		 * Refresh the project to see modifications. This is executed
		 * asynchronously because the refresh operation is waiting for the
		 * completion of the build() method to run.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					project.refreshLocal(IProject.DEPTH_INFINITE, null);
					synchronized (signal) {
						refreshed = true;
						signal.notifyAll();
					}

				} catch (CoreException e1) {
					OcamlPlugin.logError("ocaml plugin error", e1);
				}
			}
		});

		// Execute a background job to decorate files with markers
		Job job = new Job("Decorating Project") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					while (!refreshed && !monitor.isCanceled()) {
						synchronized (signal) {
							signal.wait(1000);
						}
					}

					IFile[] files = Misc.getProjectFiles(project);

					monitor.beginTask("Decorating Project", files.length + 3);

					/*
					 * Delete all markers on the project (since we rebuilt it).
					 * This can be problematic with warning markers, that will
					 * disappear at the next rebuild (since files with only
					 * warnings won't be recompiled). The warning marker will
					 * only reappear next time the file in which it appears is
					 * modified.
					 */
					try {
						project.deleteMarkers(IMarker.PROBLEM, false,
								IResource.DEPTH_INFINITE);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}

					monitor.worked(1);

					/*
					 * Parse the compiler output to find error and warning
					 * messages.
					 */
					ProblemMarkers problemMarkers = new ProblemMarkers(project);
					problemMarkers.makeMarkers2(output.toString());

					monitor.worked(1);

					// Remove the "error" and "warning" property on each project
					// file
					for (IFile f : files) {
						Misc.setFileProperty(f,
								OcamlBuilder.COMPILATION_ERRORS, null);
						Misc.setFileProperty(f,
								OcamlBuilder.COMPILATION_WARNINGS, null);
					}

					/*
					 * Put a "warning" property on files that generated at least
					 * a warning but not any error
					 */
					for (IFile f : problemMarkers.getFilesWithWarnings())
						Misc.setFileProperty(f,
								OcamlBuilder.COMPILATION_WARNINGS, "true");

					/*
					 * Put an "error" property on files that generated at least
					 * an error
					 */
					for (IFile f : problemMarkers.getFilesWithErrors())
						Misc.setFileProperty(f,
								OcamlBuilder.COMPILATION_ERRORS, "true");

					if (problemMarkers.errorsFound())
						Misc.setProjectProperty(project,
								OcamlBuilder.COMPILATION_ERRORS, "true");
					else
						Misc.setProjectProperty(project,
								OcamlBuilder.COMPILATION_ERRORS, null);

					Misc.updateDecoratorManager();

					monitor.worked(1);

					/*
					 * Mark files as byte-code or native depending on their
					 * extension.
					 */
					for (IFile file : files) {
						monitor.worked(1);

						if (monitor.isCanceled())
							break;

						String extension = file.getFileExtension();
						if ("byte".equals(extension)) {
							Misc.setFileProperty(file,
									OcamlBuilder.COMPIL_MODE,
									OcamlBuilder.BYTE_CODE);
						} else if ("native".equals(extension)) {
							Misc.setFileProperty(file,
									OcamlBuilder.COMPIL_MODE,
									OcamlBuilder.NATIVE);
						}
					}

				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error (file decorator)",
							e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		job.setPriority(Job.DECORATE);
		job.schedule(250);
	}

	/**
	 * Returns true if the delta contains modified source files (so as not to
	 * get into an infinite loop in the builder, which rebuilds because it sees
	 * its own generated files changed)
	 */
	private boolean changed = false;

	private boolean containsChanges(IResourceDelta delta) {
		changed = false;
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					IPath path = resource.getFullPath();

					// ignore files in the _build directory
					for (String segment : path.segments()) {
						if (segment.equals("_build")
								|| segment.equals(OcamlPaths.EXTERNAL_SOURCES)
								|| segment.equals(Misc.HYPERLINKSDIR))
							return true;
					}

					if (delta.getKind() == IResourceDelta.REMOVED) {
						// System.err.println(resource.getLocation().toOSString()
						// + " removed");
						changed = true;
						return false;
					}

					String ext = resource.getFileExtension();
					if (ext != null && ext.matches("ml|mli|mll|mly")) {
						// System.err.println(resource.getLocation().toOSString()
						// + " changed");
						changed = true;
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			OcamlPlugin.logError("error in ocamlbuild resource delta visitor",
					e);
			e.printStackTrace();
		}

		return changed;
	}

	@Override
	protected void startupOnInitialize() {
	}

	@Override
	protected void clean(IProgressMonitor monitor) {
		String ocamlbuild = OcamlPlugin.getOcamlbuildFullPath().trim();

		if ("".equals(ocamlbuild)) {
			OcamlPlugin.logError("ocamlbuild path is not configured");
			return;
		}

		String[] command = { ocamlbuild, "-clean" };

		String path = this.getProject().getLocation().toOSString();
		CommandRunner commandRunner = new CommandRunner(command, path);

		final String out = commandRunner.getStdout();
		final String err = commandRunner.getStderr();

		// refresh the workspace
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					OcamlCompilerOutput outputView = OcamlCompilerOutput.get();
					if (outputView != null) {
						if (!"".equals(out))
							outputView.append(out);
						if (!"".equals(err))
							outputView.append(err);
					}

					getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
				} catch (CoreException e1) {
					OcamlPlugin.logError("ocaml plugin error", e1);
				}
			}
		});
	}

}
