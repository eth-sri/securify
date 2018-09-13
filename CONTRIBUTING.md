# How to contribute

## Bugs

If you have found a bug in Securify, please:

* check that this bug has not been reported already
* minimize the Solidity code used to trigger the bug
* describe the steps to trigger the bug using files only present in this
  repository (in particular, don't rely on securify.ch only, given that the
  version used there is not necessarily the last one)

## Patterns

The patterns currently implemented are relatively short and self-explanatory,
and can be enough to get you started to write your own patterns.

In particular, the following terms may be relevant:

* the code *complies* with the pattern if Securify was able to prove the safety
  of the code with respect to this pattern.
* the code *violates* the pattern if Securify was able to prove the unsafety of
  the code with respect to this pattern.

Ideally, patterns should always be able to prove either compliance or
violation (a pattern managing to prove both for some code is inconsistent and
should be fixed).

If you managed to implement your own pattern, please provide small Solidity
examples to make them easier to understand, and implement the corresponding
tests. Again, the few tests already implemented should help you get started.


## Features

Although the current focus is mostly on improving usability, fixing bugs and
reducing the number of false positives, new features are also welcome.
