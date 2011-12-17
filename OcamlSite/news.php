<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <title>News</title>
    <?php require("header.php"); ?>
</head>
<body>
<?php require("menu.php"); ?>

<div id="page">
<h1>News</h1>

<ul>

<li><b>November 27 2011:</b> Version 1.2.14
    <ul type="circle">
        <li> FIXED issue # 7: Run as OCaml Executable does not work (invalid character ':' in a configuration name)
    </ul>

<li><b>October 29 2011:</b> Version 1.2.13
    <ul type="circle">
        <li> FIXED issue # 6: Issue 4 (Store project settings beside .project file for CVS/SVN) implementation is not
            entirely correct
    </ul>

<li><b>October 01 2011:</b> Version 1.2.12
    <ul type="circle">
        <li> FIXED issue # 3: No indication of when ocamldebug is busy
        <li> FIXED issue # 4: Store project settings beside .project file for CVS/SVN
    </ul>

<li><b>September 03 2011:</b> Version 1.2.11
    <ul type="circle">
        <li> fix for debugger cannot find capitalized source file [Dmitry Bely]
        <li> renamed occurrences of "Objective Caml" and "O'Caml" to "OCaml" (as per
            http://caml.inria.fr/ocaml/name.en.html)
        <li> added separate installable feature for OcaIDE's documentation in Eclipse
    </ul>

<li><b>August 13 2011:</b> Version 1.2.10
    <ul type="circle">
        <li> don't add directories under a directory starting by a dot to the project paths
        <li> add .svn and .git to the list of available filters
    </ul>

<li><b>May 14 2011:</b> Version 1.2.9
    <ul type="circle">
        <li> OcaIDE now supports OCaml 3.12 [Dmitry Bely]
        <li> Fixed formatting of exponential numbers [Dmitry Bely]
    </ul>

<li><b>November 29 2009:</b> Version 1.2.8
    <ul type="circle">
        <li> Added completion on the ocamlbuild targets field
        <li> Don't automatically add "_build" or ".*" folders or one of their sub-directories to project paths
        <li> Display a message box on Windows when building with Ocamlbuild and Cygwin is not installed
        <li> Fix template variables capitalization
    </ul>

<li><b>November 28 2009:</b> Version 1.2.7
    <ul type="circle">
        <li> Forgot to include the default templates file (templates.xml) in the build. Fixed!
    </ul>

<li><b>November 28 2009:</b> Version 1.2.6
    <ul type="circle">
        <li> Implemented support for editing code templates. You can now create your own templates, and import/export
            them.
        <li> Improved code template system (the cursor is now positioned where it makes most sense inside each template
            after insertion)
        <li> Fixed a StackOverflowError while parsing very big files
    </ul>

<li><b>October 17 2009</b><br>
    I have migrated the CVS OcaIDE source code repository to git, hosted on github.<br>
    git clone url: <tt>git://github.com/nbros/OcaIDE.git</tt><br>
    web interface: <a href="http://github.com/nbros/OcaIDE">http://github.com/nbros/OcaIDE</a> <br>
    Contributors are welcome!

<li><b>July 14 2009:</b> Version 1.2.5
    <ul type="circle">
        <li> Added the Quick Outline (Ctrl+O by default). Allows access to any element by name in the editor very
            quickly.
        <li> Added an action to sort the Outline view
        <li> Added an "other flags" section to ocamlbuild parameters
        <li> Added an option to disable generating type information with ocamlbuild
        <li> Fixed a problem with definition offsets, which caused some files to be unavailable for code help (see <a
            href="http://ocaml.eclipse.free.fr/forum/viewtopic.php?f=2&t=213&start=0">this forum post</a>)
        <li> Fixed a problem when an empty path was passed to ocamlbuild
        <li> Extracted help into a different plug-in to avoid forcing users to download the whole documentation each
            time an update is done, since the help isn't updated nearly as often as the binaries.
    </ul>

<li><b>January 11 2009:</b> Version 1.2.4
    <ul type="circle">
        <li> Toplevel now uses Eclipse text font (which can be changed in preferences)
        <li> Split launch configuration into two pages : main and debug
        <li> Debugger root directory is now configurable (between project root and executable directory)
        <li> Removed "Makefile Paths" properties page which was useless (see <a
            href="http://ocaml.eclipse.free.fr/forum/viewtopic.php?f=4&t=94&start=0">this forum post</a>)
        <li> Preference initializer now sets default ocamldebug path on Windows (OCaml 3.11)
        <li> Checkpoints are disabled by default on Windows since they are not supported by ocamldebug on Windows
    </ul>

<li><b>October 21 2008:</b> Version 1.2.3
    <ul type="circle">
        <li> Launch ocamldebug from the project's root directory [Dmitry Bely]
        <li> Script file support for debugger [Dmitry Bely]
        <li> Additional make options [Dmitry Bely]
        <li> Fixed exception when make target list is empty [Dmitry Bely]
        <li> Corrected spell-checking of ocamldoc comments
        <li> Restored special tab completions which had stopped working because of a bug
        <li> Fixed regression : errors not highlighted in toplevel under Windows
    </ul>

<li><b>July 20 2008:</b> Version 1.2.2
    <ul type="circle">
        <li> Support for ocamldebug under Windows, patch submitted by Dmitry Bely
        <li> Can separate run file and debugged file (patch from forum member khooyp)
        <li> Redirected messages to the console and handled uncaught exceptions (patch from forum member khooyp)
        <li> Suppressed removal of spaces before commas and semicolons inside comments and strings
    </ul>

<li><b>April 21 2008:</b> Version 1.2.1
    <ul type="circle">
        <li> Remote debugging support, patch submitted by Jonathan Knowles
        <li> Updated documentation for debugging under Windows and remote debugging
    </ul>

<li><b>April 20 2008:</b> Version 1.2.0
    <ul type="circle">
        <li> Code formatter rewrite : better formatting and more parameters
        <li> Integrated a patch for OMake support contributed by Dmitry Bely
        <li> Interface files are now parsed with a real parser, instead of the old fuzzy one
        <li> Use parsed ml files in completion when there is no corresponding mli file
        <li> Replaced linked external resources by uses of the new Eclipse File System API to open workspace-external
            files in editors. This means no more ".DebuggerSourceLookup" and ".HyperlinksLinkedFiles", which should also
            fix incompatibility with eclipsedarcs.
        <li> Can now create custom toplevels (Run As > Ocaml Toplevel)
        <li> Commenting and uncommenting of code blocks in the editor (protected and not protected)
        <li> Replaced Unix newlines by platform newlines
        <li> Added 'insert spaces instead of tabulations' option for the OCaml editor
        <li> Hyperlink on open directive opens a ".ml" file when there is no ".mli" file
        <li> Added conversion from tabs to spaces
        <li> Spell checking of comments and documentation comments
        <li> Project paths are now also contributed from referenced projects
        <li> When a folder is added or removed in an ocaml project, this project's paths are automatically changed
            accordingly
        <li> Option to override ocamlbuild tools paths
        <li> Improved OCaml paths preference page
        <li> Replaced build "N°" by corresponding Unicode character (as suggested by Ben Liblit)
        <li> Removed unnecessary protection tokenization of ocamlbuild paths, which caused problems
        <li> Removed the underscore from colorized punctuation characters
        <li> User setting for disabling automatic completion
        <li> Changed a regular expression which was causing stack overflows on big inputs
        <li> In the toplevel, Ctrl+Enter always evaluates the expression, even when it is not terminated with ";;". This
            is useful to answer input queries, or when using a different syntax (revised syntax uses a single ";" for
            example).
        <li> Changed the terminal type for toplevel, so that it doesn't try to send formatting characters
        <li> The debugger is now passed the paths defined for the project
        <li> Optimized an often used function
        <li> Keep leading and trailing newlines as-is when formatting a selection
        <li> Ctrl+Clicking on "B" in "module A = B" now opens B.mli or B.ml
        <li> Simplified synchronization in Ocamlbuild builder and makefile builder
        <li> Opening a module's/interface's counterpart now works on external resources
        <li> Fixed a layout issue in project paths properties page
        <li> Removed a check that prevented using the Cygwin version of ocamldebug on Windows
        <li> And many other small improvements and bug fixes

    </ul>

<li><b>October 14 2007:</b> Added video tutorials
<li><b>August 21 2007:</b> Version 1.1.1
    <ul type="circle">
        <li> New menu action for switching between interface and implementation
        <li> Support for camlp4 preprocessing
    </ul>

<li><b>August 02 2007:</b> Version 1.1.0
    <ul type="circle">
        <li> Added support for ocamlbuild projects (reset the OCaml perspective to get the project shortcut)
        <li> Type annotations are searched first in ml folder, then in _build + ml folder
        <li> Big change in Ocaml Browser: now you can add new locations (right-click on tree), and the loading and
            parsing is done lazily (when you expand a node)
        <li> Fixed yet another bug with hyperlinks
        <li> Fixed a bug in the lexer (with line labels)
        <li> Fixed another bug in the lexer with the '"' character inside comments
        <li> Added separate color preferences for 'let','in' and 'fun','function' keywords
        <li> Added optional syntax coloring of punctuation
        <li> Corrected a bug in the paths preference page on Windows
        <li> Added '*.obj' and '_build' in the list of navigator filters
        <li> Hid some resources from versioning system (*.cmo, *.annot, ...). If you ever need to commit these
            resources, see "Window > Preferences > Team > Ignored Resources".
        <li> Updated the online manual to explain new possibilities with the newer version.
    </ul>

<li><b>July 29 2007:</b> Update 1.0.8
    <ul type="circle">
        <li> Now, completion gives elements found in other modules even if there is no associated mli
        <li> Corrected a rare bug with syntax-coloring of ocamldoc comments
        <li> Now, a different (red) icon appears in completion results for modules that couldn't be parsed
        <li> Changed the 'let in' icon in the outline: now, the (+) icon only appears when the definition is at toplevel
        <li> The integrated lexer now supports linenum directives
        <li> Added an outline for ocamlyacc files (showing non-terminals)
        <li> Fixed a bug with non-rec definitions that would act as rec in hyperlinks
        <li> Corrected some bugs with the 'unnest let in definitions' option in outline
    </ul>


<li><b>July 27 2007:</b> Update 1.0.7
    <ul type="circle">
        <li> Completion, hyperlinks, and the module browser are now based on the new parser (major refactoring)
        <li> Drastic speed-up of hyperlinks which open other modules
        <li> Tweaked the 'unnest in' option, and corrected a bug which caused definitions to appear out of order.
        <li> Split the outline preference page into two pages, because it was too long.
        <li> Fixed a bug with definitions that appeared several times in the outline when the code was not syntactically
            correct.
        <li> Fixed a bug with syntax coloring of ocamldoc comments
        <li> If the debugger doesn't want to exit after clicking on "terminate", a second click on the "terminate"
            button will kill it, together with the debugged process
        <li> Added an option (for the editor) to automatically add the closing double-quote
        <li> "Convert revised syntax to standard syntax" and "Format with camlp4" actions now display the potential
            error message
        <li> Definitions cache now uses soft references
        <li> Added an option to not display 'and' definitions in blue in the outline
    </ul>


<li><b>July 25 2007:</b> Update 1.0.6
    <ul type="circle">
        <li> hyperlinks now find renamed modules in current module
        <li> hyperlinks now work correctly with definitions like: "let a = 2;; let a = a in let a = a in a;;"
        <li> modified the paths preference page to make it more intuitive
        <li> added an option to "unnest" "let in" definitions (activated by default)
        <li> added "while" and "begin end" templates
        <li> improved outline speed
    </ul>

<li><b>July 24 2007:</b> Update 1.0.5
    <ul type="circle">
        <li> fixed a bug that caused the debugger to get stuck when the executable being debugged was killed
        <li> completion box now automatically closes after typing a space
        <li> added a facility to change all the paths at once in the paths preference page
        <li> added an option to always expand some elements in the outline (modules, classes)
        <li> added an option (both in the preferences and in the outline toolbar) to always fully expand the outline
        <li> fixed a bug in the parser with complex patterns in 'let in' expressions
        <li> added editing support for ml4 files (but no parser and no outline)
    </ul>

<li><b>July 23 2007:</b> Update 1.0.4
    <ul type="circle">
        <li> Fixed a bug with hyperlinks that happened when a definition had the same name as one in Pervasives
        <li> Completion proposals can now look in linked resources (patch submitted by Gregory)
        <li> Syntax coloring now works correctly with comments inside strings inside comments and ocamldoc comments
            (strings must be terminated inside comments in Ocaml)
        <li> "unexpected end of line" syntax error now appears at the end of the file instead of the beginning
        <li> changed the formatter so that it doesn't add a space before a colon, since that can cause naming labels to
            become incorrect ("~a:1" became "~a : 1")
        <li> added a preference page for the formatter
        <li> added a preference page for filtering the contents of the outline
        <li> added a preference page for the debugger
        <li> updated OcamlMakefile (generic makefile) to the latest version
        <li> added "/bin" (on a Unix system, if it exists) and the Ocaml library path to the makefile paths of a new
            makefile project
        <li> fixed a bug that caused the whole interface to freeze while the outline was being rebuilt
    </ul>

<li><b>July 22 2007:</b> Update 1.0.3
    <ul type="circle">
        <li>This update fixes (hopefully!) the Java 1.5 compatibility problems.
            The previous version still wasn't compatible with Java 1.5 due to
            a bug (feature?) in the Eclipse plug-in build system: the plug-in was always
            built using the general settings instead of the project-specific ones.
        <li>The plug-in is no longer unpacked on install, since the bug which prevented
            loading of icons from the jar on Windows has been fixed.
    </ul>

<li><b>July 22 2007:</b> A forum has been created to discuss OcaIDE.

<li><b>July 21 2007:</b> Update 1.0.2
    <ul type="circle">
        <li>Plug-in is compiled with Java 1.5 compatibility options again (I forgot it in 1.0.1, sorry!).
            This should fix "Bad version number in .class file" errors.
        <li>Toplevel process now inherits parent environment
        <li>Fixed a bug in "Load in Toplevel" action under Windows
    </ul>
<li><b>July 14 2007:</b> I will be in holidays from July 15 to July 21

<li>
    <b>July 14 2007:</b> Update (1.0.1):
    (see the <a href="install.php">install</a> page for information on how to update)
    <ul type="circle">
        <li>fixed some bugs
        <li>added a compatibility option
            (in the preferences) to disable Unicode characters which can cause problems on some
            systems (namely Windows...)
        <li>added options to enable or disable displaying of types in the outline, editor popups, and
            editor's status bar.
        <li>modified the install to unpack the plug-in's jar after installation, because Windows
            couldn't find some image resources in the jar file (although this worked on other systems)
        <li>changed the generated "makefile" file in Makefile Projects to "Makefile" (capitalized)
    </ul>
</li>

<li>
    <b>July 13 2007:</b> First public release (version 1.0.0)
</li>

</ul>

</div>
</body>
</html>




