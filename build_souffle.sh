#!/usr/bin/env bash
set -e
for dl_file in smt_files/*.dl ; do
  output_file=build/$(basename -s '.dl' "$dl_file")
  if [ "$dl_file" -nt "$output_file" ]; then
    souffle --dl-program="$output_file" "$dl_file" &
  fi
done

wait
