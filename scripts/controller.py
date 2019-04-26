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
import logging

from . import solc_project
from . import truffle_project
from . import solc_file


class Controller:
    """Handles CLI arguments.
    """

    def __init__(self):
        self._parser = argparse.ArgumentParser(description='Run Securify.')
        self._parser.add_argument('-t', '--truffle',
                                  action="store_true",
                                  help="use Truffle project as base")
        self._parser.add_argument('-p', '--project',
                                  action="store", help="the project root",
                                  required=True)
        self._parser.add_argument('--json',
                                  action="store_true",
                                  help="provide JSON output to console")
        self._parser.add_argument('--descriptions',
                                  action="store_true",
                                  help="add descriptions to the JSON output")
        self._parser.add_argument('--noexiterror',
                                  action="store_true",
                                  help="do not return an error as exit code if a violation is found")
        verbosity_group = self._parser.add_mutually_exclusive_group()
        verbosity_group.add_argument('-v', '--verbose',
                                     action="store_true",
                                     help="provide verbose output")
        verbosity_group.add_argument('-q', '--quiet',
                                     action="store_true",
                                     help="suppress most output")

        self.args, securify_flags = self._parser.parse_known_args()

        # Check for single file
        if self.args.project.endswith(".sol"):
            self._project = solc_file.SolcFile(
                self.args.project, self.args, securify_flags)
        elif self.args.truffle:
            self._project = truffle_project.TruffleProject(
                self.args.project, self.args, securify_flags)
        else:
            self._project = solc_project.SolcProject(
                self.args.project, self.args, securify_flags)

        if self.args.verbose:
            level = logging.DEBUG
        elif self.args.quiet:
            level = logging.WARNING
        else:
            level = logging.INFO

        logging.basicConfig(level=level, format="%(message)s")

    def compile_and_report(self):
        """Executes securify and returns violations and warnings.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        return self._project.execute()
