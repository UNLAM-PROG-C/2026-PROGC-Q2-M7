<h1 align="center">
    Concurrent Programming Practical Work [2026]
</h1>

<p align="center">
    <strong>Practical work repository for the Concurrent Programming course</strong>
    <br>
    <strong>- <a href="https://www.unlam.edu.ar/">UNLaM</a> (National University of La Matanza) -</strong>
</p>

<p align="center">
    <a href="#summary">Summary</a> •
    <a href="#installation">Installation</a> •
    <a href="#how-to-contribute">How to contribute</a> •
    <a href="#project-structure">Project structure</a>
    <br>
    <a href="#development-team">Development team</a> •
    <a href="#additional-material">Additional material</a>
</p>

<p align="center">
	<img src="docs/statics/billboard.png" alt="Billboard" width="800">
</p>

## Summary

This repository contains our practical work for the Concurrent Programming course at the [National University of La Matanza (UNLaM)](https://www.unlam.edu.ar/). It is a recreation of [LinkedIn's Zip game](https://www.linkedin.com/help/linkedin/answer/a7445030), developed in Java with multithreading and concurrency concepts.

## Installation

1. Clone the repository and install [SDKMAN!](https://sdkman.io/).
2. Open a terminal, navigate to the repository folder, and run `sdk env install`.
3. Run the `scripts/health-check.sh` script to check that all the necessary tools are installed.
4. Run the `scripts/run.sh` script to run the project.
5. That's it! A window should open with the project running.

## How to contribute

1. Clone the repository.
2. Install [Visual Studio Code](https://code.visualstudio.com/), [Docker](https://www.docker.com/), and [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).
3. Open the repository folder in Visual Studio Code.
4. Reopen the project in a Dev Container, pressing `F1` and selecting `Dev Containers: Rebuild and Reopen in Container`.
5. Wait for the container to be built and started.
6. That's it! You are ready to contribute to the project.

> [!NOTE]
> To run the project, you must follow the steps in the [installation section](#installation) (avoid the first step), as the Dev Container can not render the project window.

## Project structure

```bash
TP-Integrador/
│
├── .devcontainer/        # DevContainer configuration files.
│   └── devcontainer.json # DevContainer definition (tools, extensions, and settings).
│
├── docs/                 # Documentation files, such as diagrams, images, and other statics.
│   ├── statics/          # Static assets used by the documentation.
│   │   └── billboard.png # Repository billboard image.
│   └── arquitectura.html # Interactive architecture walkthrough.
│
├── scripts/
│   ├── build.sh          # Script to build the project.
│   ├── health-check.sh   # Script to check that the necessary tools are installed.
│   └── run.sh            # Script to run the project.
│
├── src/
│   ├── utils/            # Package with utility classes and functions.
│   │
│   └── Main.java         # Entry point of the project.
│
├── .editorconfig         # Configuration to standardize code style across different IDEs.
├── .gitattributes        # Configuration for Git attributes.
├── .gitignore            # List of all files and folders that Git should ignore.
├── .sdkmanrc             # Configuration file for SDKMAN! to manage Java versions.
└── README.md             # This file.
```

## Development team

- [Farias Maira](https://github.com/maifarias)
- [Hoz Lucas](https://github.com/hozlucas28)
- [Loiero Federico](https://github.com/FNLoiero)
- [Massa Valentin](https://github.com/ValentinMassa)
- [Niedfeld Kevin](https://github.com/KevinNied)
- [Zapata Santiago](https://github.com/Santiago01011)

## Additional material

