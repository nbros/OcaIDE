<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
    <title>News</title>
    <?php require("header.php"); ?>
</head>
<body>
<?php $current="sources"; require("menu.php"); ?>

<div id="page">

    <h1>Sources</h1>

    Sources are now available on <a href="http://github.com/nbros/OcaIDE">GitHub</a>.
    <p>
        An archive containing the latest version of the sources can also be <a
        href="http://github.com/nbros/OcaIDE/archives/master">downloaded from GitHub</a>.

    <p>

        The sources are also bundled inside the plug-in. You can see them and modify the plug-in inside Eclipse.
        To do so, if the plug-in is correctly installed in your Eclipse, follow these steps:
    <ul>
        <li>Click on <b>File &gt; Import</b></li>
        <li>Choose <b>Plug-ins and Fragments</b> in the <b>Plug-in Development</b> category</li>
        <li>Check <b>The target platform</b>, click on <b>Select from all plug-ins and fragments found
            at the specified location</b> and <b>Projects with source folders</b></li>
        <li>Click <b>Next</b></li>
        <li>Move the <b>Ocaml</b> plug-in from the left column to the right one, by selecting
            it and clicking <b>Add</b></li>
        <li>Finally, click <b>Finish</b> to import the plug-in source into your development environment</li>
    </ul>

</div>
</body>
</html>
