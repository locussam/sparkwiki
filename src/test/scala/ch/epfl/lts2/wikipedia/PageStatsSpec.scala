package ch.epfl.lts2.wikipedia

import org.scalatest._
import org.scalactic._
import java.time._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, Row, DataFrame, SparkSession, Dataset}
import java.sql.Timestamp

//case class PageVisitGroup(page_id:Long, visits:List[(Timestamp, Int)])
class PageStatsSpec extends FlatSpec with SparkSessionTestWrapper {
  
   val epsilon = 1e-6f

  implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)
  
  "PageStats" should "compute mean and variance correctly" in {
    val ps = new PageStats("localhost", 9042)
    //case class PageVisitElapsedGroup(page_id:Long, visits:List[(Int, Double)])
    val p = PageVisitElapsedGroup(1, List((1, 1.0), (10, 1.0)))
    val size = 20
    val r = ps.computeStats(p, size)
  
    assert(r.page_id == p.page_id)
    assert(r.mean === 0.1)
    assert(r.variance === 0.0947368421)
    
    val p2 = PageVisitElapsedGroup(2, List((0, 1.0), (3, 1.0), (10, -1.0), (13, 2.0), (16, -5.0)))
    
    val r2 = ps.computeStats(p2, size)
    assert(r2.page_id == p2.page_id)
    assert(r2.mean === -0.1)
    assert(r2.variance === 1.6736842105)
  }
  "PeakFinder" should "compute mean and variance correctly" in {
    import spark.implicits._
    val pf = new PeakFinder("localhost", 9042, "keyspace", "tableVisits", "tableStats", "boltUrl", "neo4j", "neo4j")
    val startDate = LocalDate.parse("2018-08-01")
    val endDate = LocalDate.parse("2018-08-01")
    val p1 = PageVisitGroup(1, 
                            List((Timestamp.valueOf(startDate.atTime(1, 0, 0)), 11), 
                                 (Timestamp.valueOf(startDate.atTime(20, 0, 0)), 1)
                                )
                            )
    val p2 = PageVisitGroup(2, 
                            List((Timestamp.valueOf(startDate.atTime(1, 0, 0)), 1), 
                                 (Timestamp.valueOf(startDate.atTime(3, 0, 0)), 1),
                                 (Timestamp.valueOf(startDate.atTime(17, 0, 0)), 1),
                                 (Timestamp.valueOf(startDate.atTime(20, 0, 0)), 2),
                                 (Timestamp.valueOf(startDate.atTime(22, 0, 0)), -5)
                                )
                            )                            
    val plist = List(p1, p2).toDS()
    val res = pf.getStats(plist, startDate, endDate)
    
    val res1 = res.filter(_.page_id == 1).first
    assert(res1.mean === 0.5)
    assert(res1.variance === 5.04347826)
    
    val res2 = res.filter(_.page_id == 2).first
    assert(res2.mean === 0.0)
    assert(res2.variance === 1.39130435)
    
  
  }
}

