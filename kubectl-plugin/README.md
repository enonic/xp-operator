<h1>Enonic XP kubectl plugin</h1>

This is a plugin to help you with common operations when running XP with the operator.

- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)

## Requirements

* kubectl
* bash
* jq
* fzf

## Installation

Clone this repository and add the `kubectl-plugin` to your `PATH`, i.e:

```console
$ git clone https://github.com/enonic/xp-operator.git
$ cd `xp-operator/kubectl-plugin`
$ export PATH="$PATH:$PWD"
```

To make this permanent you need to set your path in your environment, i.e. in you .bashrc file.

## Usage

```console
$ kubectl enonic
The enonic kubectl plugin.
You can invoke it through kubectl: "kubectl enonic [command]..."

Find more information at: https://github.com/enonic/xp-operator

Usage:
  kubectl enonic [command]

Available Commands:
  operator      Enonic XP Operator commands
  xp            Enonic XP commands
```