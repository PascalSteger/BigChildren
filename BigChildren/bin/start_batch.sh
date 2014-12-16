#!/bin/bash
# start batch script for all weather stations of a country

countryID=$1

for i in ../resources/$countryID/*/
do
    station=$(echo $i|rev|cut -d"/" -f2|rev)
    ./restart.sh $1 $station
    ./extract_country.sh $1 > ../resources/$countryID/corr
done
