#!/usr/bin/env bash
set -e

command -v souffle > /dev/null
mkdir -p build
pids=""
for dl_file in smt_files/*.dl ; do
  output_file=src/main/resources/$(basename -s '.dl' "$dl_file")
  if [ "$dl_file" -nt "$output_file" ]; then
    # this will still leave the .cpp in case of compilation failure
    (souffle --dl-program="$output_file" "$dl_file" && rm "$output_file".cpp) &
    pids+="$! "
  fi
done

for pid in $pids ; do
  wait $pid || exit 1
done
