#!/bin/bash
sshpass -p "$1" rsync -e "ssh -o StrictHostKeyChecking=no" -r --delete "$2" "$3"
