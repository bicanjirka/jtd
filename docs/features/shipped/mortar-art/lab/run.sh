#!/bin/bash
# Renders the mortar art into docs/features/shipped/mortar-art. From the repo root, after `mvn -q compile` and with
# target/runtime-classpath.txt (see the run-jtd skill): bash docs/features/shipped/mortar-art/lab/run.sh
# The lab's TowerFoundations shadows the game's on the classpath, so render before the game's builder changes, or
# check out 8b3a32b6's target/classes.
set -e
L=target/mortar-lab
mkdir -p $L/src/td/ui $L/classes
python docs/features/shipped/mortar-art/lab/assemble.py
cp docs/features/shipped/mortar-art/lab/MortarLab.java.txt $L/src/td/ui/MortarLab.java
CP="target/classes;$(cat target/runtime-classpath.txt)"
javac -nowarn -cp "$CP" -d $L/classes $L/src/td/ui/*.java
java -Djava.awt.headless=true -cp "$L/classes;$CP" td.ui.MortarLab docs/features/shipped/mortar-art
