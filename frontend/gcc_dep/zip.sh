#!/bin/bash
cd /home/ubuntu/gcc/gcc_dep/exec
cd "$1"
cd executions
zip -r ../../../temp/"$3" "$2"
