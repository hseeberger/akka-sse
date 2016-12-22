#!/usr/bin/env sh

sbt "jmh/jmh:run -i 10 -wi 10 -f 2 -t 1" >> jmh-reports/$(git describe --tags)
