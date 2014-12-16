#!/bin/bash
countryID=$1
countryString=$(head -n1 ../resources/$1/childmortality.txt | cut -d" " -f4- | rev| cut -d" " -f 2-|rev)

for i in ../resources/$countryID/*/
do
    station=$(echo $i|rev|cut -d"/" -f2|rev)
    #echo $station
    askstr=$(echo '{"index" : "')$(echo $countryString)$(echo "_")$(echo $station)$(echo '"}')
    fullstr=$(echo "'")$(echo $askstr)$(echo "'")

    echo "#!/bin/bash">tmpfile
    echo $(echo 'curl -d ')$fullstr$(echo " 'http://localhost:10000/v2/apps/BigChildren/procedures/CorrelationsProcedure/methods/Correlations'; echo")>>tmpfile
    sh tmpfile
done
