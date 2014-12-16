#!/bin/sh
L=`readlink -f $0`
L=`dirname $L`/lib
clp=`echo $L/*.jar|sed 's/ /:/g'`
echo $clp
#/usr/bin/env scala -classpath $cp $0 $@
exit
