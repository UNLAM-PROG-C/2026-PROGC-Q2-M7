#! /bin/bash

# Cambiar del directorio del script a la raiz del proyecto
cd "$(dirname "$0")/.."

if [[ $? -ne 0 ]]; then
	printf "\e[31mNo se pudo acceder a la raiz del proyecto.\e[0m\n" >&2
	exit 1
fi

# Salir ante cualquier fallo
set -euo pipefail

rm -rf build/classes
mkdir -p build/classes

fuentes=()
while IFS= read -r -d '' fuente; do
	fuentes+=("$fuente")
done < <(find src -name '*.java' -print0)

javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes "${fuentes[@]}"
jar --create --file build/app.jar --main-class Principal -C build/classes .

printf '\e[32m- Compilacion completada correctamente.\e[0m\n'
