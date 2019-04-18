#!/usr/bin/env python3

"""
Authors: Tobias Kaiser, Jakob Beckmann

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
import contextlib
import logging
import os
import sys


def find_node_modules_dir(directory):
    """Returns the path of the node module folder contained in directory.
    """
    for x in os.walk(directory):
        if os.path.isdir(os.path.join(x[0], 'node_modules')):
            return os.path.join(x[0], 'node_modules')
    return None



def handle_process_output_and_exit(error):
    """Processes stderr from a process error.
    """
    if error.stdout:
        logging.fatal(error.stdout.strip())
    sys.exit(1)


@contextlib.contextmanager
def working_directory(path):
    """Changes current directory to path.
    """
    prev_cwd = os.getcwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(prev_cwd)


