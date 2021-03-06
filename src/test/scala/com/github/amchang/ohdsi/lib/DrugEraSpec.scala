package com.github.amchang.ohdsi.lib

import com.typesafe.config.Config
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}

/**
  * Test for DrugEra
  */
class DrugEraSpec extends RDDSpec with BeforeAndAfter with MockitoSugar with BeforeAndAfterAll {

  // setup values
  implicit var conf: Config = null
  implicit var sparkCont: SparkContext = null
  var sqlCont: SQLContext = null

  var drugEra: DrugEra = null
  var drugEraData: List[((Int, Int, String, String), List[(DateTime, DateTime)])] = List()
  val dateStringFormat = "yyyyMMdd"
  var dateStringFormatter: DateTimeFormatter = null

  override protected def beforeAll() = {
    super.beforeAll
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("drug_era_spec")
      .setMaster("local")
    conf = mock[Config]
    sparkCont = new SparkContext(sparkConf)
    sqlCont = new SQLContext(sparkCont)

    dateStringFormatter = DateTimeFormat.forPattern(dateStringFormat)
    when(conf.getString("ohdsi.dateFormat")).thenReturn(dateStringFormat)
  }

  override protected def afterAll() = {
    sparkCont.stop
  }

  before {
    drugEra = new DrugEra()

    val createInitialData = classOf[DrugExposure].getDeclaredField("createInitialData")
    createInitialData.setAccessible(true)
    // stub the data here
    createInitialData.set(drugEra, () => {
      sparkCont.parallelize(drugEraData)
    }: RDD[((Int, Int, String, String), List[(DateTime, DateTime)])])
  }

  describe("build") {
    it("returns an empty RDD") {
      assert(drugEra.build().isEmpty)
    }

    it("returns a singular RDD") {
      val firstDate = "20080605"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 1)
      assert(result(0) ==(0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        1, 0))
    }

    it("returns an RDD with two non overlapping ranges") {
      val firstDate = "20080605"
      val secondDate = "20080331"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 2)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        1, 0)
      )
      assert(result(1) == (0, 948078,
        dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate),
        1, 0)
      )
    }

    it("returns an RDD with two non overlapping ranges yet there are three ranges but two are equal") {
      val firstDate = "20080605"
      val secondDate = "20080331"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 2)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        1, 0)
      )
      assert(result(1) == (0, 948078,
        dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate),
        2, 0)
      )
    }

    it("returns an RDD with two ranges, one overlapping") {
      val firstDate = "20080605"
      val secondDate = "20080331"
      val thirdDate = "20080410"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(thirdDate), dateStringFormatter.parseDateTime(thirdDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 2)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        1, 0)
      )
      assert(result(1) == (0, 948078,
        dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(thirdDate),
        2, 10)
      )
    }

    it("returns an RDD with three ranges, one overlapping") {
      val firstDate = "20080605"
      val secondDate = "20080331"
      val thirdDate = "20080410"
      val fourthDate = "20100901"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(fourthDate), dateStringFormatter.parseDateTime(fourthDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(secondDate)))
        ),
        (
          (0, 948078, "", ""),
          List((dateStringFormatter.parseDateTime(thirdDate), dateStringFormatter.parseDateTime(thirdDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 3)
      // sort is not random unlike the original one
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(fourthDate), dateStringFormatter.parseDateTime(fourthDate),
        1, 0)
      )
      assert(result(1) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        1, 0)
      )
      assert(result(2) == (0, 948078,
        dateStringFormatter.parseDateTime(secondDate), dateStringFormatter.parseDateTime(thirdDate),
        2, 10)
      )
    }

    it("returns an RDD for two dates that start / end on each other") {
      val firstDate = "20080605"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 1)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate),
        2, 0)
      )
    }

    it("returns an rdd for two ranges that start but don\'t end at the same time non stockpile") {
      val firstDate = "20080605"
      val secondDate = "20080625"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 1)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate),
        2, 0)
      )
    }

    it("returns an rdd for two ranges that start but don\'t end at the same time stockpile") {
      val firstDate = "20080605"
      val secondDate = "20080625"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate)))
        )
      )

      val result = drugEra.build(true).collect

      assert(result.length == 1)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate),
        2, -1)
      )
    }

    it("returns an rdd for three ranges that start but don\'t end at the same time non stockpile") {
      val firstDate = "20080605"
      val secondDate = "20080625"
      val fourthDate = "20080710"
      val fifthDate = "20080725"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(fourthDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(fifthDate), dateStringFormatter.parseDateTime(fifthDate)))
        )
      )

      val result = drugEra.build().collect

      assert(result.length == 1)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(fifthDate),
        3, 15)
      )
    }

    it("returns an rdd for three ranges that start but don\'t end at the same time stockpile") {
      val firstDate = "20080605"
      val secondDate = "20080625"
      val fourthDate = "20080710"
      val fifthDate = "20080725"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(fourthDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(fifthDate), dateStringFormatter.parseDateTime(fifthDate)))
        )
      )

      val result = drugEra.build(true).collect

      assert(result.length == 1)
      assert(result(0) == (0, 903963,
        dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(fifthDate),
        3, -5)
      )
    }
  }

  describe("writeCSV") {
    it("does nothing considering it has nothing to write") {
      assert(drugEra.writeCSV.isEmpty)
    }

    it("writes a csv file out") {
      import java.io.{File => JFile}
      import better.files._

      val firstDate = "20080605"
      val secondDate = "20080625"
      drugEraData = List(
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(firstDate)))
        ),
        (
          (0, 903963, "", ""),
          List((dateStringFormatter.parseDateTime(firstDate), dateStringFormatter.parseDateTime(secondDate)))
        )
      )

      when(conf.getString("ohdsi.csv.location")).thenReturn("/tmp/")

      drugEra.build()

      val result = drugEra.writeCSV.get
      val resultFile = File(s"${result}/part-00000")

      assert(resultFile.exists)

      val lines = resultFile.lines.toArray
      assert(lines(0) == "drug_era_id,person_id,drug_concept_id,drug_era_start_date,drug_era_end_date,drug_exposure_count,gap_days")
      assert(lines(1) == "0,0,903963,20080605,20080625,2,0")
    }
  }

}
