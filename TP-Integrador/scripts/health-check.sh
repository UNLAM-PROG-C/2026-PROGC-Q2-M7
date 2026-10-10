#! /bin/bash

# Parse options
while [[ $# -gt 0 ]]; do
	case "$1" in
		-h | --help)
			need_help='true'
			shift 1
			break
			;;

		--)
			shift 1
			break
			;;

		-*)
			printf "\e[31mAn invalid option was found!\e[0m\n" >&2
			exit 1
			;;

        *)
            break
            ;;
	esac
done

# Show help if needed
if [[ -n "$need_help" ]]; then
	printf "Usage: $0 [OPTIONS]...

Perform a health check of the tools needed to this project.

Options:
	-h, --help     display this help and exit
"
	exit 0
fi

# Gets command version
command_version() {
	local version

	if version=$("$@" 2> /dev/null); then
		echo "v$(echo "$version" | grep -E -o '[0-9]+(\.[0-9]+){0,2}' | head -n 1)"
	else
		return 1
	fi
}

# Change from script directory to project root directory
cd "$(dirname "$0")/.."

if [[ $? -ne 0 ]]; then
	printf "\e[31mFailed to change directory to project root.\e[0m\n" >&2
	exit 1
fi

# Exit on any command failure
set -e

exit_code=0


tools=("java" "javac" "jar")

if command -v java > /dev/null 2>&1; then
	printf "\e[32m- Java $(command_version java --version) installed.\e[0m\n"
else
	printf "\e[31m- Java is not installed or not found in PATH.\e[0m\n" >&2
	exit_code=1
fi

if command -v jar > /dev/null 2>&1; then
	printf "\e[32m- JAR $(command_version jar --version) installed.\e[0m\n"
else
	printf "\e[31m- JAR is not installed or not found in PATH.\e[0m\n" >&2
	exit_code=1
fi

if command -v javac > /dev/null 2>&1; then
	printf "\e[32m- JavaC $(command_version javac --version) installed.\e[0m\n"
else
	printf "\e[31m- JavaC is not installed or not found in PATH.\e[0m\n" >&2
	exit_code=1
fi


exit $exit_code
