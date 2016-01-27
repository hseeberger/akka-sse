#!/usr/bin/env sh

report=$(git describe --tags)
sbt "akkaSseJmh/jmh:run -i 10 -wi 10 -f 2 -t 1" > jmh-reports/$report

