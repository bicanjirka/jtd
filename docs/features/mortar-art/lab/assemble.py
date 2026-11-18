# Builds the lab's copy of TowerFoundations: the game's file at the commit the art was drawn on, with the strider
# builder appended. Run from the repo root: python docs/features/mortar-art/lab/assemble.py
import subprocess
base = subprocess.run(['git', 'show', '8b3a32b6:src/main/java/td/ui/TowerFoundations.java'], capture_output=True,
                      check=True).stdout.decode('utf-8').rstrip()[:-1]
lab = open('docs/features/mortar-art/lab/strider-mortar-builder.java.txt', encoding='utf-8').read()
open('target/mortar-lab/src/td/ui/TowerFoundations.java', 'w', encoding='utf-8').write(base + lab)
