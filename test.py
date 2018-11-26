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


class OutputMismatchException(Exception):
    """Raised when there is a mismatch between past and current output
    """
    pass


def test_securify_analysis(c_file, json_output, memory=4, overwrite=False):
    """Compare the output of Securify on c_file with its expected output

    Args:
        c_file: the contract to analyze
        json_output: the expected json_output
        memory: the number of GB to use
        overwrite: whether to overwrite the output in case of mismatch
    """
    mem_available = psutil.virtual_memory().available // 1024 ** 3
    assert mem_available >= memory, f'Not enough memory to run: {mem_available}G'

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
            raise exn

        if overwrite:
            print('Overwriting.')
            shutil.copy(output, json_output)
            return

        with open(output) as fsc:
            current_output = json.load(fsc)

        with open(json_output) as fsj:
            expected_output = json.load(fsj)

        if current_output != expected_output:
            print('Different output!')
            print('Current output:')
            print(current_output)
            print('Expected output:')
            print(expected_output)
            raise OutputMismatchException


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
    print('Done.')


if __name__ == '__main__':
    unit = Path('src/test/resources/solidity')
    test(unit)
    swc = Path('src/test/resources/solidity/swc_registry')
    test(swc)
    quick = Path('src/test/resources/solidity/end_to_end_testing_quick')
    test(quick)
