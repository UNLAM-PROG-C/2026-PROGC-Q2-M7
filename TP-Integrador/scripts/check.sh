#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

version="10.26.1"
suma_control="e41c24433723ba310a30e41da4f449c105ad47cab2ae9e6be5e06606a647dbca"
herramienta="build/tools/checkstyle-${version}-all.jar"
mkdir -p build/tools
if [[ ! -f "$herramienta" ]]; then
	curl --fail --location --show-error \
		"https://github.com/checkstyle/checkstyle/releases/download/checkstyle-${version}/checkstyle-${version}-all.jar" \
		--output "${herramienta}.download"
	printf '%s  %s\n' "$suma_control" "${herramienta}.download" | shasum -a 256 -c -
	mv "${herramienta}.download" "$herramienta"
fi
printf '%s  %s\n' "$suma_control" "$herramienta" | shasum -a 256 -c -
java -jar "$herramienta" -c config/checkstyle.xml src test
