"""
Compare the output of the current Securify with past outputs.

Author: Quentin Hibon

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
import json
import argparse
import sys
import os.path

from pathlib import Path

import psutil
from scripts import pysolc
from scripts import solc_file



class MismatchError(Exception):
    """Raised when there is a mismatch between past and current output
    """
    pass


def raise_mismatch(expected, current, pat=None):
    """Raises the formatted exception.
    """
    msg = [f'Current: {current}', f'expected: {expected}']

    if pat is not None:
        msg.append(f'pattern: {pat}')

    raise MismatchError(', '.join(msg))


def check_every_match(curr_res, exp_res, contract, pat,
                      keys=('violations', 'warnings', 'safe', 'conflicts')):
    """Checks every match of a given pattern.
    """
    for k in keys:
        c_pat_results, e_pat_results = curr_res[pat][k], exp_res[pat][k]
        if c_pat_results != e_pat_results:
            print(f'Different output in {contract} '
                  f'with pattern {pat} with {k}!')
            raise_mismatch(e_pat_results, c_pat_results, pat)


def check_all_patterns(curr_json, expc_json, contract):
    """Checks every pattern from the given json
    """
    res = 'results'
    try:
        curr_res = curr_json[contract][res]
    except KeyError:
        curr_res = ""
    exp_res = expc_json[contract][res]

    for pat in exp_res:
        if pat in curr_res:
            check_every_match(curr_res, exp_res, contract, pat)
        else:
            raise MismatchError(f'Pattern {pat} is missing '
                                'from the new results.')


def equal_string_modulo_digits(s1, s2):
    """Returns whether two strings without their digits are the same
    """
    s1 = (c for c in s1 if not c.isdigit())
    s2 = (c for c in s2 if not c.isdigit())

    return all(c1 == c2 for c1, c2 in zip(s1, s2))


def check_securify_errors(curr_json, expc_json, contract):
    """Checks that the error output is the same.
    """
    err = 'securifyErrors'
    try:
        curr_errors = curr_json[contract][err]
    except KeyError:
        curr_errors = ""

    expc_errors = expc_json[contract][err]

    # we don't care if the line of the error changed in the Java source
    if not equal_string_modulo_digits(curr_errors, expc_errors):
        print('Different Securify errors:')
        raise_mismatch(expc_errors, curr_errors)


def rewrite_json_output(curr_json, outj):
    with open(outj, 'w') as fso:
        json.dump(curr_json, fso, sort_keys=True, indent=2)


class MyArgs(object):
    def __init__(self):
        self.json = True
        self.descriptions = False
        self.verbose = False
        self.quiet = True


def test_securify_analysis(c_file, json_output, args):
    """Compare the output of Securify on c_file with its expected output

    Args:
        c_file: the contract to analyze
        json_output: the expected json_output
        args: overwrite, printdiffs, memory
    Returns: Whether changes were found
    """
    mem_available = psutil.virtual_memory().available // 1024 ** 3
    if mem_available < args.memory:
        raise AssertionError(f'Not enough memory to run: {mem_available}G')

    if args.printdiffs:
        outfname = "/tmp/testdiffs/"  + os.path.basename(json_output)

    project = solc_file.SolcFile(c_file, MyArgs(), "")

    curr_json = project.execute_for_json()

    try:
        with open(json_output) as fsj:
            expc_json = json.load(fsj)
    except FileNotFoundError:
        if args.overwrite:
            print(f'Creating {json_output}.')
            rewrite_json_output(curr_json, json_output)
            return True

        if args.printdiffs:
            print(f'Storing diffs for {json_output}.')
            with open(outfname, "w") as diff:
                json.dump(curr_json, diff)
            return True

        raise AssertionError('We should never get here')

    # If different contracts were detected
    if set(curr_json.keys()) != set(expc_json.keys()):
        raise MismatchError(f'Different contracts detected for {c_file}')

    for contract in curr_json:
        try:
            check_securify_errors(curr_json, expc_json, contract)
            check_all_patterns(curr_json, expc_json, contract)
        except MismatchError as e:
            if args.overwrite:
                print(f'Overwriting {json_output}.')
                rewrite_json_output(curr_json, json_output)
                return True
            if args.printdiffs:
                print(f'Storing diffs for {json_output}.')
                with open(outfname, "w") as diff:
                    diff.write(json.dumps(curr_json))
                return True
            raise e
    return False



def test(tests_dir, args, recursive=False):
    """Run all the tests in the given path

    Args:
        tests_dir: the path in which to look for .sol and .json files
        args: overwrite, printdiffs, memory
    """
    find = tests_dir.rglob if recursive else tests_dir.glob
    changes_found = False
    for contract_file in find('*.sol'):
        print(f'Running on {contract_file}')
        json_output = contract_file.with_suffix('.json')
        if not (json_output.exists() or args.overwrite or args.printdiffs):
            raise AssertionError(f'Missing {json_output}')
        changes_found |= test_securify_analysis(contract_file, json_output, args)
    print(f'Done with files in folder: {tests_dir}.')
    return changes_found

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run Securify Tests.')
    parser.add_argument('-o', '--overwrite',
                                action="store_true",
                                help="Overwrite previous results")
    parser.add_argument('-d', '--printdiffs',
                                action="store_true",
                                help="Write all diffs to disk")
    parser.add_argument('-m', '--memory',
                                action="store_true",
                                help="Number of GB to use")
    args = parser.parse_args()

    changes_found = False

    UNIT = Path('src/test/resources/solidity')
    changes_found |= test(UNIT, args)
    QUICK = Path('src/test/resources/solidity/end_to_end_testing_quick')
    changes_found |= test(QUICK, args)
    BIG = Path('src/test/resources/solidity/end_to_end_testing_big')
    changes_found |= test(BIG, args, recursive=True)
    print('Done.')
    sys.exit(changes_found)
