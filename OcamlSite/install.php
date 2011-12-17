<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <title>Installing</title>
    <?php require("header.php"); ?>
</head>
<body>
<?php require("menu.php"); ?>

<div id="page">

    <h1>Installing</h1>

    <h2>Prerequisites</h2>
    OcaIDE needs a 1.5 or 1.6 Java Virtual Machine (from Sun: <a href="http://java.com/en/download/index.jsp">download
    the JRE here</a>)
    and <a href="http://www.eclipse.org/downloads/">Eclipse 3.5</a> to work correctly.
    <p>

        It will <b>not</b> work with a JVM version inferior to 1.5.

    <p>

        If you want to use OcaIDE with an old version of Eclipse, either grab the jar from <a href="eclipse32/">here</a>
        (old version)
        and install it manually, or recompile it from source:
        <a href="eclipse32.html">Compiling OcaIDE for Eclipse 3.2</a>.

    <p>

        Also, the plug-in has been reported not to work with GCJ (since GCJ doesn't completely support Java 1.5 yet)


    <p>

        It works best on Linux and Mac OS X, and some features are unavailable under Windows
        (debugger checkpoints and interrupting the toplevel, mainly).


    <h2>Installation</h2>
    To install OcaIDE inside of Eclipse, open Eclipse, click on <b>Help &gt; Install New Software...</b>

    <p>

        Enter <b>http://www.algo-prog.info/ocaide/</b> as the update site.

    <p>
        Check the OcaIDE category or feature in the list.

    <p>
        Then, click <b>Next</b> twice, accept the agreement and click <b>Finish</b>.

    <p>

        The plug-in then downloads... It can take a while depending on your connection speed.

    <p>

        Accept the installation (the plug-in is not digitally signed), and restart Eclipse when it asks you to.

    <p>

        As soon as Eclipse is restarted, you can start using the plug-in.

    <p>

        You can access the online help by clicking on <b>Help &gt; Help Contents</b> in the Eclipse main menu,
        and choosing <b>OCaml Development User Guide</b> in the list of available topics.

    <h2>Updating</h2>

    You can update the plug-in by clicking on <b>Help &gt; Check for Updates</b> in Eclipse main
    menu. The plug-in should appear in the list of available features if there is an update available.

    <h2>Manual Installation</h2>

    If for a reason you can't install the plug-in through the "Software Updates" system,
    you can still install it manually:
    <ul>
        <li>Download the jar file from <a href="plugins">here</a>.
        <li>Put it in one of your "plugins" directory (for example, "/usr/lib/eclipse/plugins/").
        <li>Restart Eclipse.
        <li>If the plug-in is not detected, try to start Eclipse with the "-clean" option (on command line).
    </ul>

    <h2><a name="uninstall">Uninstalling</a></h2>

    To uninstall the plug-in, click on <b>Help &gt; About Eclipse SDK</b>
    in Eclipse main menu. Then, click on the <b>Installation Details</b> button.
    In the <b>Installed Software</b> tab, select the OcaIDE feature and click <b>Uninstall...</b>.

</div>
</body>
</html>