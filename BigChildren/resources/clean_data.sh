#!/bin/bash

# call via
# ./clean_data.sh CC       where CC is the country ID, eg MI for Mali

# output is in each stations folder, eg MI/612020/ :
# allheader:  sample header
# alldata:    concatenated data from all years

echo $1
cd $1

# remove NaN values, replace them with common -1 value
find . -type f -print0 | xargs -0 sed -i 's/99999/-1/g'
find . -type f -print0 | xargs -0 sed -i 's/9999\.9/-1/g'
find . -type f -print0 | xargs -0 sed -i 's/999\.9/-1/g'
find . -type f -print0 | xargs -0 sed -i 's/99\.99/-1/g'
find . -type f -print0 | xargs -0 sed -i 's/ \+ / /g'


# concat all data files

# first read first header, and store it into
for i in *;
do
    head -n1 $i/$(ls -l $i|head -n 2|tail -n 1|rev|cut -d" " -f 1|rev) > $i/allheader
done;

for i in *;
do
    echo "" > $i/alldata;
    for j in $i/*.op;
    do
        tail -n +2 $j &>> $i/alldata;
    done;
done;
