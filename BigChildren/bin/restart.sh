#!/bin/bash

CBIN=/home/ubuntu/cdap2/cdap-sdk-2.5.0/bin/
BBIN=/home/ubuntu/Big_data/BigChildren/bin/

echo "truncate stream weatherStream" | $CBIN/cdap-cli.sh
echo "truncate stream childmortalityStream" | $CBIN/cdap-cli.sh

echo "truncate dataset instance Weather"| $CBIN/cdap-cli.sh
echo "truncate dataset instance ChildMortality"|$CBIN/cdap-cli.sh
#cdap-cli.sh truncate dataset instance Correlations

echo "stop flow BigChildren.WeatherFlow" | $CBIN/cdap-cli.sh
echo "stop flow BigChildren.ChildMortalityFlow"|$CBIN/cdap-cli.sh

echo "start flow BigChildren.WeatherFlow"|$CBIN/cdap-cli.sh
echo "start flow BigChildren.ChildMortalityFlow"|$CBIN/cdap-cli.sh

$BBIN/inject-data.sh --country $1 --station $2
sleep 7

$BBIN/app-manager.sh --action run --country $1 --station $2
sleep 120
