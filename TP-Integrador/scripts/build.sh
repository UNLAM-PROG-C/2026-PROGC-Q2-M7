#! /bin/bash

# Change from script directory to project root directory
cd "$(dirname "$0")/.."

if [[ $? -ne 0 ]]; then
	printf "\e[31mFailed to change directory to project root.\e[0m\n" >&2
	exit 1
fi

# Exit on any command failure
set -euo pipefail

rm -rf build/classes
mkdir -p build/classes

sources=()
while IFS= read -r -d '' source; do
	sources+=("$source")
done < <(find src -name '*.java' -print0)

javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes "${sources[@]}"
jar --create --file build/app.jar --main-class Main -C build/classes .

printf '\e[32m- Build completed successfully.\e[0m\n'
