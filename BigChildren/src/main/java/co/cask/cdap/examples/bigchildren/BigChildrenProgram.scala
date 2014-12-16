/*
 * GPL v.3 psteger@phys.ethz.ch
 * Main class, implements mean temperature calculation and cross-correlation
 */

package co.cask.cdap.examples.bigchildren

import breeze.linalg.DenseVector
import breeze.linalg.Vector
import breeze.stats.meanAndVariance
import breeze.stats.mean
import breeze.stats.variance

// http://docs.cdap.io/cdap/current/en/reference-manual/javadocs/index.html
import co.cask.cdap.api.spark.ScalaSparkProgram
import co.cask.cdap.api.spark.SparkContext

// https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.package
import org.apache.spark.SparkContext._

// https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.rdd.NewHadoopRDD
import org.apache.spark.rdd._ //NewHadoopRDD

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.math

import org.apache.commons.math3._
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation

//import java.io.PrintWriter
//import java.io.FileWriter

/**
  * main class
  */
class BigChildrenProgram extends ScalaSparkProgram {
  //var status: PrintWriter = new PrintWriter(new FileWriter("/home/psteger/logger", true))
  private final val LOG: Logger = LoggerFactory.getLogger(classOf[BigChildrenProgram])

  final val interval = 7 // [days]
  def parseTemp(fahrenheit: Double): Double = {
    (fahrenheit + 459.67)*5/9
  }

  def parsePrec(inches: Double): Double = {
    return inches*25.4
  }

  // we split a weather CSV file line by single whitespaces here, and convert parts to SI
  def parsewVector(line: String): Tuple3[Double,Double,Double] = {
    var dv = DenseVector(line.split(" ", -1))
    if(dv.length != 22) {
      return (-1, -1, -1)
    }
    //STN--- WBAN YEARMODA TEMP 4 DEWP 4 SLP    4 STP 0 VISIB 4 WDSP 4 MXSPD GUST    MAX    MIN   PRCP   SNDP     FRSHTT
    //612230 -1   19570701 92.8 4 66.0 4 1008.5 4 -1  0 14.0  4 3.5  4 8.0   -1      106.0  82.0  0.00I  -1       000000

    val timestamp: Double = (dv(2)).toDouble
    val temperature: Double = parseTemp(   (dv(3)).toDouble )
    if(!(dv(19)).charAt((dv(19)).length-1).isLetter){
      return (-1, -1, -1)
    }
    val precip: Double = parsePrec(  (dv(19)).substring(0, math.max(2, (dv(19)).length()-1)).toDouble)
    return (timestamp, temperature, precip)
  }


  // extract station ID from weather dataset
  def parsesVector(line: String): Int = {
    var dv = DenseVector(line.split(" ", -1))
    if(dv.length != 22) {
      return -1
    }
    //STN--- ...
    //612230...
    return (dv(0)).toInt
  }

  // extract country name from mortality data
  def parsecVector(line: String): String = {
    LOG.info("parsecVector with String: "+line)
    val dv = DenseVector(line.split(" ", -1))
    val country_entries = dv.slice(3, dv.length-1, 1)
    var country: String = ""
    for(i <- 0 until country_entries.length){
      country = country+country_entries(i)
      if(country_entries.length>1){
        country = country + " "
      }
    }
    return country
  }



  // extract year and mortality data from dataset
  def parsemVector(line: String): Tuple2[Double,Double] = {
    LOG.info("parsemVector with String: "+line)
    val dv = DenseVector(line.split(" ", -1))
    LOG.info("parsemVector after splitting")
    val year: Double = (dv(0)).toDouble // year
    LOG.info("parsemVector determined year: "+year.toInt.toString)
    val mort: Double = (dv(dv.length-1)).toDouble // mortality per 1000 children
    LOG.info("parsemVector determined mortality: "+mort.toString)
    return (year, mort)
  }

  // find which interval a given date YYYY-MM-DD lies in
  // by counting all days since start of the year, and projecting into 52 bins
  def findInterval(dateID: Int, w: Double): Int = {
    val Y: Int = dateID/10000
    val M: Int = (dateID - Y*10000)/100
    println(M)
    val Day: Int = (dateID - Y*10000)%100
    //LOG.info(Y, M, Day)
    var MonthLength = Array(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if ((Y%4 == 0) && (Y%100 != 0 || Y%400 == 0)) {
      MonthLength(1) += 1     // add Feb 29 if necessary
    }
    // add up all completed months
    var dayofyear: Int = 0
    for(i <- 1 until M) {
      println(i)
      dayofyear = dayofyear+MonthLength(i-1)
    }
    // add days since beginning of current month
    dayofyear  += Day
    // map into week number (each of which is 7 days long)
    val out: Int = ((dayofyear-1)/w).toInt
    return out
  }

  def weight(y: Int, yi: Int, sig: Double, w: Double): Double = {
    val out: Double = math.exp(-math.pow(y-yi,2)/(2*math.pow(sig,2)))/(math.sqrt(2*math.Pi)*sig) //*365.25/w
    return out
  }





  override def run(sc: SparkContext) {
    LOG.info("Reading in weather data")
    // new API Hadoop RDD according to apache.org website,
    // not directly instantiating HadoopRDD
    val linesDataset: NewHadoopRDD[Array[Byte], String] =
      sc.readFromDataset("Weather", classOf[Array[Byte]], classOf[String])

    // split by whitespaces, convert to Vector[Double], convert to SI units
    LOG.info("parsing vectors")
    val wdata0 = linesDataset.map(kv => parsewVector(kv._2)).cache()
    LOG.info("length of wdata0: "+wdata0.count().toString)
    val station = parsesVector(linesDataset.first()._2)
    LOG.info("station number is "+station)

    LOG.info("stripping misformatted entries")
    val wdata = wdata0.filter(kv => ( (kv._1) >= 0))
    LOG.info("length of wdata, after stripping: "+wdata.count().toString)

    // cycle through all datasets
    // get datestamp, and corresponding period (say, week, interval=7days) of interest
    // get temperature from col (0,1,2,)3, calc new mean
    // get precipitation from col 13, calc new mean

    // do this via a Map-Reduce scheme
    // first get key value as period to hash to,
    // then extract temperature and precipitation


    // floor(p(2)/10000) gives year from timestamp 20140321
    val tdat = wdata.map(p => ( findInterval((p._1).toInt,interval), ((math.floor(p._1/10000)).toInt, p._2)))
      .filter( kv => ((kv._2._2) > 0.0 ))
    LOG.info("tdat length: "+tdat.count().toString)
    val pdat = wdata.map(p => (findInterval((p._1).toInt,interval), ((math.floor(p._1/10000)).toInt, p._3)))
      .filter( kv => ((kv._2._2) >= 0.0 ))
    LOG.info("pdat length: "+pdat.count().toString)

    LOG.info("filtered data")

    LOG.info("tred")
    // use the values in tred twice, thus we store it separately
    val tred = tdat.map(kv => (kv._1, kv._2._2)).groupByKey()
    LOG.info("tred length: "+tred.count().toString)

    LOG.info("tMean")
    val tMean = tred.map(kv => (kv._1, mean(kv._2)))
    LOG.info("tMean length: "+tMean.count().toString)

    LOG.info("tSig")
    val tSig  = tred.map(kv => (kv._1, math.max(0.00001, math.sqrt(variance(kv._2)))))
    LOG.info("tSig length: "+tSig.count().toString)



    LOG.info("pred")
    val pred = pdat.map(kv => (kv._1,  kv._2._2)).groupByKey()
    LOG.info("pred length: "+pred.count().toString)
    LOG.info("pMean")
    val pMean = pred.map(kv => (kv._1, mean(kv._2)))
    LOG.info("pMean length: "+pMean.count().toString)
    LOG.info("pSig")
    val pSig  = pred.map(kv => (kv._1, math.max(0.00001, math.sqrt(variance(kv._2)))))
    LOG.info("pSig length: "+pSig.count().toString)



    // run through all values again, calculate distance from mean
    // do the join for each time interval
    // and store a new key, value pair, where key is the year
    // Mahalanobis distance
    LOG.info("tDiff")
    val tDiff = tdat.join(tMean)
      .map(kv => (kv._1, (kv._2._1._1, math.abs(kv._2._1._2-kv._2._2))))
    // has key #interval, values (key: #year, diff from mean)
    LOG.info("tDiff length: "+tDiff.count().toString)
    LOG.info("pDiff")
    val pDiff = pdat.join(pMean).map(kv => (kv._1, (kv._2._1._1, math.abs(kv._2._1._2-kv._2._2))))
    LOG.info("pDiff length: "+pDiff.count().toString)



    // outlierness of any week, still for all years
    LOG.info("tOut")
    val tOut  = tDiff.join(tSig)
      .map(kv => (kv._2._1._1, kv._2._1._2/kv._2._2))
    // which is year, and diff/sig
    LOG.info("tOut length: "+tOut.count().toString)
    LOG.info("pOut")
    val pOut  = pDiff.join(pSig)
      .map(kv => (kv._2._1._1, kv._2._1._2/kv._2._2))
    LOG.info("pOut length: "+pOut.count().toString)
    // has new key #year, values (diff from mean / sig)



    // determine outlierness of a given year, summing over all time intervals this time
    LOG.info("tweather_out")
    val tweather_out = tOut.groupByKey().map(kv => (kv._1, kv._2.sum))
    LOG.info("tweather_out length: "+tweather_out.count().toString)
    LOG.info("pweather_out")
    val pweather_out = pOut.groupByKey().map(kv => (kv._1, kv._2.sum))
    LOG.info("pweather_out length:"+pweather_out.count().toString)



    // read in mortality data
    LOG.info("read in mortality")
    val mortality: NewHadoopRDD[Array[Byte], String] =
      sc.readFromDataset("ChildMortality", classOf[Array[Byte]], classOf[String])
    val country = parsecVector(mortality.first()._2)
    LOG.info("country is "+country)

    // split by whitespaces, convert to Vector[Double]
    LOG.info("parse mdata")
    val mdata = mortality.map(kv => parsemVector(kv._2)).cache()
    LOG.info("length of mdata: "+mdata.count().toString)

    LOG.info("m_out")
    val m_out = mdata.map(p => ( (p._1).toInt, p._2)    )
    LOG.info("length of m_out: "+m_out.count().toString)



    val tmiddle = tweather_out.join(m_out).sortByKey(true)
    LOG.info("length of tmiddle: "+tmiddle.count().toString)

    val toutsort = tmiddle.map(kv => kv._2._1).collect()
    LOG.info("length of toutsort: "+toutsort.length.toString)
    val tmoutsort = tmiddle.map(kv => kv._2._2).collect()
    LOG.info("length of tmoutsort: "+tmoutsort.length.toString)

    // determine direct correlation as in Pearson; time-shift assumed
    // small, no ARIMA needed
    LOG.info("before tcorr")
    val tcorr = (new PearsonsCorrelation()).correlation(toutsort, tmoutsort)
    LOG.info("Correlation tcorr for "+country+", station "+station.toString+": "+tcorr.toString)



    val pmiddle = pweather_out.join(m_out).sortByKey(true)
    LOG.info("length of pmiddle: "+pmiddle.count().toString)

    val poutsort = pmiddle.map(kv => kv._2._1).collect()
    LOG.info("length of poutsort: "+poutsort.length.toString)
    val pmoutsort = pmiddle.map(kv => kv._2._2).collect()
    LOG.info("length of pmoutsort: "+pmoutsort.length.toString)

    LOG.info("before pcorr")
    val pcorr = (new PearsonsCorrelation()).correlation(poutsort, pmoutsort)
    LOG.info("Correlation pcorr for "+country+", station "+station.toString+": "+pcorr.toString)





    LOG.info("Done... writing to Correlations RDD")
    // write to DataSet, for output later
    val output_array = Array(station.toDouble, pcorr, tcorr)
    var corrs = new Array[(Array[Byte], String)](1)
    corrs(0) = Tuple2((country+"_"+station).getBytes, output_array.mkString(","))
    val originalContext: org.apache.spark.SparkContext = sc.getOriginalSparkContext()
    var outputrdd = originalContext.makeRDD[(Array[Byte], String)](corrs)
    sc.writeToDataset(outputrdd, "Correlations", classOf[Array[Byte]], classOf[String])

  }
}
