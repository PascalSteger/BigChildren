#!/bin/bash
# get all station numbers:
cat ../resources/$1/corr |sort|uniq |head -n-1 > ../resources/$1/corr_nonull
cat ../resources/$1/corr_nonull | sed "s/^\"\(.*\)\.0,\(.*\)\$/\1/g" > ../resources/$1/stnwithcorr

NEWFILE=../resources/$1/popcorr
echo -n > $NEWFILE

totpopd=0

FILE=../resources/$1/stnwithcorr
while read stnnr; do
    echo $stnnr
    storedcorrs=$(grep $stnnr ../resources/$1/corr_nonull|sed 's/^"\(.*\)"/\1/g'|sed 's/,/ /g')
    pcorr=$(echo $storedcorr|cut -d" " -f2)
    tcorr=$(echo $storedcorr|cut -d" " -f3)
    storedpopd=$(grep " "$stnnr" " ../resources/stnpop_noheader_100km_space.txt|rev|cut -d" " -f1|rev)
    count=$(echo $storedpopd|wc -m)
    echo $count


    if [ $count -gt 1 ]
    then
	echo $storedcorrs $storedpopd >> $NEWFILE
	
	totpopd=$(echo $totpopd+$storedpopd|bc)
        echo "totpopd:" $totpopd
    fi
done < "$FILE"


wpcorr=0.0
wtcorr=0.0
while read corrpop; do
    echo $corrpop
    pcorr=$(echo $(echo $corrpop)|cut -d" " -f2)
    #echo "pcorr " $pcorr
    tcorr=$(echo $(echo $corrpop)|cut -d" " -f3)
    #echo "tcorr " $tcorr
    pop=$(echo $(echo $corrpop)|cut -d" " -f4)
    #echo "pop " $pop

    #echo $wpcorr+$pop/$totpopd*$pcorr
    wpcorr=$(echo $(echo "scale=6;"$wpcorr"+"$pop"/"$totpopd"*"$pcorr)|bc)
    #echo "wpcorr " $wpcorr
    wtcorr=$(echo $(echo "scale=6;"$wtcorr+$pop/$totpopd*$tcorr)|bc)
    echo $wpcorr $wtcorr
done < "$NEWFILE"

echo $wpcorr $wtcorr > ../resources/$1/weighted_correlations
