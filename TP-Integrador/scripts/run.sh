#! /bin/bash

# Change from script directory to project root directory
cd "$(dirname "$0")/.."

if [[ $? -ne 0 ]]; then
	printf "\e[31mFailed to change directory to project root.\e[0m\n" >&2
	exit 1
fi

# Exit on any command failure
set -euo pipefail

# Build the application
bash scripts/build.sh

# Run the application
exec java -jar build/app.jar "$@"
