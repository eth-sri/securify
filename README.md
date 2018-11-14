# Securify

![securify](/img/foundation_securify.png)

Securify is a security scanner for Ethereum smart contracts supported by the
[Ethereum
Foundation](https://ethereum.github.io/blog/2018/08/17/ethereum-foundation-grants-update-wave-3/)
and [ChainSecurity](https://chainsecurity.com). The core
[research](https://files.sri.inf.ethz.ch/website/papers/ccs18-securify.pdf)
behind Securify was conducted at the [ICE Center](http://ice.ethz.ch) at ETH
Zurich.


[![scan now](/img/scan.png)](https://securify.chainsecurity.com/)


It features an extensive list of security patterns commonly found in smart
contracts:

* some forms of the DAO bug (also known as reentrancy)
* locked ether
* missing input validation
* transaction ordering-dependent amount, receiver and transfer
* unhandled exceptions
* unrestricted ether flow


The project is meant to be an open platform welcoming contributions from all of the
Ethereum Security Community. To suggest new patterns, to volunteer for testing or to
contribute developing new patterns please get in touch through our
[Discord](https://discord.gg/nN77ckb) group.

## Getting Started
### Requirements

* Soufflé: https://github.com/souffle-lang/souffle/releases (Securify should work with
  the latest package, please raise an issue if it does not). If you cannot
  install Soufflé, look at the Docker container for an alternative. Securify
  will crash without the `souffle` binary.
  As of writing, Soufflé is not available on Windows, so Securify should not be
  expected to run on Windows either.
* Java 8
* A `solc` binary is required to be able to use Solidity file as input.
  Securify assumes that the right version is installed for the given file.
  `solc` is available [here](https://github.com/ethereum/solidity/releases).
* Linux users must have the [timeout](https://linux.die.net/man/1/timeout) command, OS X users must have the `gtimeout` command, which can be install with `brew install coreutils`.

### Use

To build:
```sh
./gradlew jar
```

To run Securify on a Solidity file:
```sh
java -jar build/libs/securify-0.1.jar -fs src/test/resources/solidity/transaction-reordering.sol
```

To run Securify on the decompilation output provided by the [pysolc.py
script](scripts/pysolc.py) (which requires py-solc):
```sh
java -jar build/libs/securify-0.1.jar -co out.json
```

To run Securify on some EVM binary (produced e.g. by `solc`):
```sh
java -jar build/libs/securify-0.1.jar -fh src/test/resources/solidity/transaction-reordering.bin.hex
```

To run the tests (which use JUnit4):
```sh
./gradlew test
```

#### Via Python 3 Script

Alternatively, you can use the python script to use Securify with more flexibility. This requires additional installs:

Install python dependencies:

```
pip3 install --user py-solc termcolor
```

Moreover, in order to compile using Truffle, please also install it using:

```
sudo npm install -g truffle
```

The usage of the `run_securify.py` script is then as follows:

```
usage: run_securify.py [-h] [-t] [-p PROJECT] [-v | -q]

Run securify.

optional arguments:
  -h, --help            show this help message and exit
  -t, --truffle         use truffle project as base
  -p PROJECT, --project PROJECT
                        the project root
  -v, --verbose         provide verbose output
  -q, --quiet           suppress most output
```

The `project` flag allows to define the path to the project you wish to analyze. In general, the exact same behaviour can be obtained using the docker image, without the hassle of the installs.

This will also provide more readable output than running Securify as a `.jar` file.

### Docker

The installation should be simple enough on Debian derivatives, or any other
platform supported by Soufflé.

For a quick demonstration which does not require Soufflé, you can use Docker.

Build the Docker image:
```sh
docker build . -t securify
```

Run Securify on a small example:
```sh
docker run securify
```

You can change the files analyzed by specifying a volume to mount, and every
`*.sol` file contained will then be processed by Securify:
```sh
docker run -v $(pwd)/folder_with_solidity_files:/project securify
```

Adding a `--truffle` flag should allow Securify to run over Truffle project in which dependencies
have already been installed (so run `npm install` before if need be).

The indices of the lines matched are 0-based, meaning that a match to line `i`
means that the `i+1`th line is matched. In particular, the first line has an
index of 0.


### Travis

You can add the following `.travis.yml` to your project to run Securify on new
commits:
```
services:
  - docker

before_install:
  - docker pull chainsecurity/securify

script:
- docker run -v $(pwd):/project chainsecurity/securify
```

### Output

The output is a in JSON and gives the vulnerabilities found over the files
analyzed and the corresponding line numbers.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

Join our [Discord](https://discord.gg/nN77ckb) to discuss with other users.

## Known Limitations

Although Securify is regularly used to help audits at ChainSecurity, there are
still bugs, including:
* the code in  the fallback function is currently not analyzed. A workaround is
  to name this function instead.
* in some cases, a StackOverflowError exception is thrown, due to
  `computeBranches` being non tail-recursive (but recursive). In most cases,
  it is enough to increase the stack size using the `-Xss` option of `java`,
  e.g. `java -Xss1G -jar ...`.
* libraries are not properly supported
* abstract contracts (whose binary cannot be obtained via `solc`) are not
  supported

## Presentations, research, and blogs about Securify

* [Securify research paper](https://files.sri.inf.ethz.ch/website/papers/ccs18-securify.pdf) to be presented at [ACM CCS'18](https://www.sigsac.org/ccs/CCS2018/) in Toronto, Canada
* [TechCrunch: This smart contract scanner will ensure your token is tip-top](https://techcrunch.com/2018/07/02/this-smart-contract-scanner-will-ensure-your-token-is-tip-top/)
* [Talk at EPFL in Lausanne](https://www.youtube.com/watch?v=YNbj_JElzuc)
* [Talk at Devcon3 in Cancún](https://www.youtube.com/watch?v=ewOEEpJs-pg)
* [Talk at d10e in Davos](https://www.youtube.com/watch?v=FO2E8x1yptg)
* [Researchers from ETH Zürich release a free state-of-the-art security scanner for Ethereum smart contracts](https://medium.com/chainsecurity/researchers-release-a-free-state-of-the-art-security-scanner-for-ethereum-smart-contracts-b58b57ce0e38)
* [Automated security analysis of Ethereum smart contracts](https://medium.com/chainsecurity/researchers-release-a-free-state-of-the-art-security-scanner-for-ethereum-smart-contracts-b58b57ce0e38)
* [Automatically detecting the bug that froze Parity's wallets](https://medium.com/chainsecurity/automatically-detecting-the-bug-that-froze-parity-wallets-ad2bebebd3b0)

## Technical details

Securify statically analyzes the EVM code of the smart contract to infer
important semantic information (including control-flow and data-flow facts)
about the contract. This step is fully automated using
[Soufflé](https://souffle-lang.github.io/), a scalable Datalog solver. Then,
Securify checks the inferred facts to discover security violations or prove the
compliance of security-relevant instructions.

The full technical details behind the Securify scanner are available in the
[research
paper](https://files.sri.inf.ethz.ch/website/papers/ccs18-securify.pdf).
