Big_data
========

Big Data - WHO &amp; Amazon Weather Data

1 Motivation
------------

Famine in Central Africa is caused by scarce supply of nutrition. This
in turn is caused by

- missing arable land
- inefficient agriculture
- malnutritious food
- disastrous weather
- malfunctioning distribution system

The effects of famine are widespread and not scope of this work. We
concentrate on one proxy, child mortality.

With the help of big-data methods, we want to answer the question
whether weather influences translate into famine and whether this is
the main influence on child mortality.

Given the results, we will be able to tell where efforts need to be
put to better the lives of the uncounted numbers of hungry people.

2 Installation
--------------

You need a scala compiler and a current Java runtime environment, as well as a hadoop environment (we propose 2.4.1) and spark (1.1.0).

Clone the repository using

> git clone https://github.com/EndreElv/Big_data

then go to where the source files reside

> cd Big_data/BigChildren

compile the sample code

> mvn clean
> mvn compile
> mvn package
> mvn install

and run it, after starting hadoop file system and CDAP, with

> cd bin

> ./app-manager.sh --action deploy

> ./app-manager.sh --action start

> ./app-manager.sh --action run

> ./inject-data.sh

Then check out the CDAP dashboard, e.g. if set on the local machine under
http://localhost:9999. After this, you might wish to stop the application:

> ./app-manager.sh --action stop


3 TODOs
-------

- aggregating data week- and season-wise,

- determining mean temperature and precipitation values for weeks 1-52
  in each year and for seasons 1-4 each year,

- using linear regression of the seasonal means over the years to get
  a baseline on climate change,

- calculating the differences between the running week's (temperature,
  precipitation) and the baseline,

- defining a metric for outlier years: We define two numbers d_T, d_P
  as the distance of this week's temperature and precipitation and the
  baseline, divided by the annual variation of said quantities, which
  yields a real number >= 0,
  and adding up these quantifiers via

  d = \sqrt{a_T d_T^2 + a_P d_P^2}

  with weights a_T = a_P = 1 in a first step,

- calculating the outlier metric for each meteorological station,

- checking the correlation between outlier years and mortality rate
  for each year.


4 Bugs
------

None known so far.

(c) 2014 GPL v.3 E. Elvestad, P. Steger (psteger@phys.ethz.ch), N. Tschurr
