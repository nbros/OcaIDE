#!/bin/bash
SOURCE="/cygdrive/c/Users/Nicolas/git/OcaIDE/OcamlSite"
TMP=/tmp/OcaIDESite

mkdir -p $TMP
rm -rf $TMP/*

cd $SOURCE
cp -r *.html *.php *.jar site.xml plugins/ features/ $TMP

cd $TMP

rm -rf plugins/CVS
rm -rf features/CVS
rm -rf plugins/.cvsignore
rm -rf features/.cvsignore

chmod -R 755 *
tar czf site.tgz *


scp site.tgz nicolas@www.algo-prog.info:
ssh nicolas@www.algo-prog.info ./deploy

rm -rf $TMP/*