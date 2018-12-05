#!/usr/bin/env python3

"""
Author: Jakob Beckmann

Copyright 2018 ChainSecurity AG

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import argparse

from . import solc_project
from . import truffle_project
from . import utils


class Controller:
    def __init__(self):
        """Initialise the controller. This sets up the command line argument parsing etc."""
        self._parser = argparse.ArgumentParser(description='Run securify.')
        self._parser.add_argument('-t', '--truffle',
                                  action="store_true",
                                  help="use truffle project as base")
        self._parser.add_argument('-p', '--project',
                                  action="store", help="the project root",
                                  required=True)
        self._parser.add_argument('--json',
                                  action="store_true",
                                  help="provide JSON output")
        verbosity_group = self._parser.add_mutually_exclusive_group()
        verbosity_group.add_argument('-v', '--verbose',
                                     action="store_true",
                                     help="provide verbose output")
        verbosity_group.add_argument('-q', '--quiet',
                                     action="store_true",
                                     help="suppress most output")

        self.args = self._parser.parse_args()

        if self.args.truffle:
            self._project = truffle_project.TruffleProject(self.args.project, self.args.json)
        else:
            self._project = solc_project.SolcProject(self.args.project, self.args.json)

        if self.args.verbose:
            utils.set_logger_level("info")
        elif self.args.quiet:
            utils.set_logger_level("error")
        else:
            utils.set_logger_level("warning")

    def compile_and_report(self):
        """Executes securify and returns violations and warnings.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        return self._project.execute()
