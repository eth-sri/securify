#!/bin/bash

if [ "$#" -ne 1 ]; then
	echo "Usage $1 <sol file>"
fi

SECURIFY="build/libs/securify.jar"

SOLFILE=$1
DIR=$(dirname $SOLFILE)

INP="$DIR/inp"
mkdir -p $INP

LOG="$(echo $SOLFILE | sed 's/\.sol/\.log/')"

echo $LOG
echo $INP
echo $SOLFILE

echo "java -jar $SECURIFY -fs $SOLFILE > $LOG"
java -jar $SECURIFY -fs $SOLFILE > $LOG

MUST=$(cat $LOG | grep mustExplicit | awk '{print $3}')
MAY=$(cat $LOG | grep mayImplicit | awk '{print $3}')

echo "Copy inputs"

# check consistency
declare -a SAMEPREDS=("assignType.facts" "assignVar.facts" "call.facts" "caller.facts" "endIf.facts" "jumpCond.facts" "mload.facts" "mstore.facts" "sha3.facts" "sload.facts" "sstore.facts" "unk.facts")

for s in "${SAMEPREDS[@]}"; do
	echo "diff $MUST/$s $MAY/$s"
	diff $MUST/$s $MAY/$s
	delta=$(diff $MUST/$s $MAY/$s | wc -l)
	if [ $delta -gt 0 ]; then
		echo "Files $same differ for may and must inputs"
		exit -1
	fi
done

cp $MUST/* $INP

for f in $MAY/*; do 
	size="$(wc -c <"$f")"; 
	if [ $size -gt 0 ]; then 
		echo "cp $f $INP"
		cp $f $INP
	fi; 
done
