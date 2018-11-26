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
import shutil
import subprocess
import tempfile

from pathlib import Path

import psutil


class MismatchError(Exception):
    """Raised when there is a mismatch between past and current output
    """
    pass


def raise_mismatch(expected, current, pat=None):
    """Raises the formatted exception.
    """
    msg = [f'Current: {current}', f'expected: {expected}']

    if pat is not None:
        msg.append('pattern: {pat}')

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
    curr_res = curr_json[contract][res]
    exp_res = expc_json[contract][res]

    for pat in exp_res:
        if pat in curr_res:
            check_every_match(curr_res, exp_res, contract, pat)
        else:
            raise MismatchError(f'Pattern {pat} is missing '
                                'from the new results.')


def check_securify_errors(curr_json, expc_json, contract):
    """Checks that the error output is the same.
    """
    err = 'securifyErrors'
    curr_errors = curr_json[contract][err]
    expc_errors = expc_json[contract][err]

    if curr_errors != expc_errors:
        print('Different Securify errors:')
        raise_mismatch(expc_errors, curr_errors)


def test_securify_analysis(c_file, json_output, memory=4, overwrite=False):
    """Compare the output of Securify on c_file with its expected output

    Args:
        c_file: the contract to analyze
        json_output: the expected json_output
        memory: the number of GB to use
        overwrite: whether to overwrite the output in case of mismatch
    """
    mem_available = psutil.virtual_memory().available // 1024 ** 3
    assert mem_available >= memory, ('Not enough memory to run: '
                                     f'{mem_available}G')

    with tempfile.TemporaryDirectory() as tmpdir:
        output = Path(tmpdir) / 'sec_output.json'

        cmd = ['java',
               f'-Xmx{memory}G',
               '-jar', 'build/libs/securify-0.1.jar',
               '-fs', c_file,
               '-o', output]

        try:
            print('Running:')
            print(' '.join(str(o) for o in cmd))
            subprocess.check_output(cmd, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as exn:
            print('Securify already failed during execution!')
            print(exn.output.decode('utf-8'))
            raise exn

        if overwrite:
            print('Overwriting.')
            shutil.copy(output, json_output)
            return

        with open(output) as fsc:
            curr_json = json.load(fsc)

        with open(json_output) as fsj:
            expc_json = json.load(fsj)

    for contract in expc_json:
        check_securify_errors(curr_json, expc_json, contract)
        check_all_patterns(curr_json, expc_json, contract)


def test(tests_dir, overwrite=False):
    """Run all the tests in the given path

    Args:
        tests_dir: the path in which to look for .sol and .json files
        overwrite: whether to write the (new) .json output
    """
    for contract_file in tests_dir.glob('*.sol'):
        print(f'Running on {contract_file}')
        json_output = contract_file.with_suffix('.json')
        assert (json_output.exists() or overwrite), f'Missing f{json_output}'
        test_securify_analysis(contract_file, json_output, overwrite=overwrite)
    print(f'Done with files in folder: {tests_dir}.')


if __name__ == '__main__':
    UNIT = Path('src/test/resources/solidity')
    test(UNIT)
    QUICK = Path('src/test/resources/solidity/end_to_end_testing_quick')
    test(QUICK)
    print('Done.')
