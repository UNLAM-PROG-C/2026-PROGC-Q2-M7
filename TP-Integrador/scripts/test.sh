#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ $# -gt 1 || ( $# -eq 1 && "$1" != "--ui" ) ]]; then
	printf 'Uso: bash scripts/test.sh [--ui]\n' >&2
	exit 1
fi

bash scripts/build.sh
rm -rf build/test-classes
mkdir -p build/test-classes
fuentes=()
while IFS= read -r -d '' fuente; do
	fuentes+=("$fuente")
done < <(find test -name '*.java' -print0)
javac --release 17 -encoding UTF-8 -Xlint:all -Werror \
	-cp build/classes -d build/test-classes "${fuentes[@]}"

ruta_clases="build/classes:build/test-classes"
java -Djava.awt.headless=true -cp "$ruta_clases" zip.dominio.PruebaDominio
java -Djava.awt.headless=true -cp "$ruta_clases" zip.infraestructura.PruebaSesion
if [[ "${1:-}" == "--ui" ]]; then
	java -cp "$ruta_clases" zip.interfaz.PruebaInterfaz
fi
