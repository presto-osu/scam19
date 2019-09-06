#!/bin/bash

for log in log/*.log; do
  pkg=${log:4:-4}
  echo $log ' >>> ' $pkg
  cat $log | grep "Monkey with seed" | sed 's/.*: //g' > seed/$pkg.seed
  cat seed/$pkg.seed | wc -l
done
