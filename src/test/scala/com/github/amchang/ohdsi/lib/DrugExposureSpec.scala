package com.github.amchang.ohdsi.lib

import java.lang.reflect.Field

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql._
import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._


/**
  * Test out DrugExposure
  */
class DrugExposureSpec extends FunSpec with BeforeAndAfter with MockitoSugar with PrivateMethodTester with BeforeAndAfterAll {

  // entire base data
  val sparkConf: SparkConf = new SparkConf()
    .setAppName("drug_exposure_spec")
    .setMaster("local")
  val conf = mock[Config]
  val sparkCont: SparkContext = new SparkContext(sparkConf)
  val sqlCont: SQLContext = new SQLContext(sparkCont)

  // schemas
  val drugExposureSchema = StructType(List(
    StructField("id", StringType, true),
    StructField("person_id", StringType, true),
    StructField("drug_concept_id", StringType, true),
    StructField("exposure_start_date", StringType, true),
    StructField("exposure_end_date", StringType, true), // 4
    StructField("foo", StringType, true),
    StructField("bar", StringType, true),
    StructField("foo1", StringType, true),
    StructField("bar1", StringType, true), // 8
    StructField("days_supply", StringType, true),
    StructField("bar2", StringType, true),
    StructField("foo3", StringType, true),
    StructField("dose_value", StringType, true),
    StructField("unit_concept_id", StringType, true) // 13
  ))

  val conceptAncestorSchema = StructType(List(
    StructField("ancestor_concept_id", StringType, true),
    StructField("descendant_concept_id", StringType, true)
  ))

  val conceptSchema = StructType(List(
    StructField("id", StringType, true),
    StructField("foo", StringType, true),
    StructField("bar", StringType, true),
    StructField("vocabulary_id", StringType, true),
    StructField("concept_class", StringType, true)
  ))

  var drugExposure: DrugExposure = null
  var loadDrugExposure: Field = null
  var loadConceptAncestor: Field = null
  var loadConcept: Field = null
  var createInitialData: () => RDD[((Int, Int, String, String), List[(DateTime, DateTime)])] = null

  // stub the cahce file location
  when(conf.getString("ohdsi.cache.location")).thenReturn("/tmp")

  before {
    drugExposure = new DrugExposure {
      val config: Config = conf
      val sparkContext: SparkContext = sparkCont
      val sqlContext: SQLContext = sqlCont
    }

    val drugExposureClass = classOf[DrugExposure]
    val allFields: Array[Field] = drugExposureClass.getDeclaredFields

    val createInitData: Field = allFields(0)
    createInitData.setAccessible(true)
    createInitialData = createInitData.get(drugExposure).asInstanceOf[() => RDD[((Int, Int, String, String), List[(DateTime, DateTime)])]]

    // setup all of the initial data here
    loadDrugExposure = allFields(2)
    loadDrugExposure.setAccessible(true)

    loadConceptAncestor = allFields(3)
    loadConceptAncestor.setAccessible(true)

    loadConcept = allFields(4)
    loadConcept.setAccessible(true)

  }

  override protected def afterAll() = {
    sparkCont.stop()
  }

  describe("DrugExposure") {

    describe("createInitialData") {
      it("returns an empty rdd since drug exposure is empty") {
        loadDrugExposure.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List())
          val struct = StructType(List())

          sqlCont.createDataFrame(numList, struct)
        }: DataFrame)

        assert(createInitialData().count == 0)
      }

      it("returns an empty rdd since no concept or concept ancestor ids could be found") {

        loadDrugExposure.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List(
             Row("1", "10", "20", "20200419", "20200421", "", "", "", "", "", "", "", "100", "30"),
             Row("2", "11", "21", "20200413", "", "", "", "", "", "20", "", "", "200", "30")
          ))
          sqlCont.createDataFrame(numList, drugExposureSchema)
        }: DataFrame)

        loadConceptAncestor.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List())

          sqlCont.createDataFrame(numList, conceptAncestorSchema)
        }: DataFrame)

        loadConcept.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List())

          sqlCont.createDataFrame(numList, conceptSchema)
        }: DataFrame)

        assert(createInitialData().count == 0)
      }

      it("returns an empty rdd since no concept ancestor ids count be found") {
        loadDrugExposure.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List(
            Row("1", "10", "20", "20200419", "20200421", "", "", "", "", "", "", "", "100", "30"),
            Row("2", "11", "21", "20200413", "", "", "", "", "", "20", "", "", "200", "30")
          ))
          sqlCont.createDataFrame(numList, drugExposureSchema)
        }: DataFrame)

        loadConceptAncestor.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List())

          sqlCont.createDataFrame(numList, conceptAncestorSchema)
        }: DataFrame)

        loadConcept.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List(
            Row("89", "", "", "RxNorm", "Ingredient"),
            Row("103", "", "", "RxNorm", "Ingredient"),
            Row("120", "", "", "RxNorm", "Ingredient"),
            Row("23", "", "", "Diag", "Screening")
          ))

          sqlCont.createDataFrame(numList, conceptSchema)
        }: DataFrame)

        assert(createInitialData().count == 0)
      }

      it("returns an empty rdd since no concept ids could be found") {
        loadDrugExposure.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List(
            Row("1", "10", "20", "20200419", "20200421", "", "", "", "", "", "", "", "100", "30"),
            Row("2", "11", "21", "20200413", "", "", "", "", "", "20", "", "", "200", "30")
          ))
          sqlCont.createDataFrame(numList, drugExposureSchema)
        }: DataFrame)

        loadConceptAncestor.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List())

          sqlCont.createDataFrame(numList, conceptAncestorSchema)
        }: DataFrame)

        loadConcept.set(drugExposure, () => {
          val numList: RDD[Row] = sparkCont.parallelize(List(
            Row("103", "9012"),
            Row("120", "890")
          ))

          sqlCont.createDataFrame(numList, conceptSchema)
        }: DataFrame)

        assert(createInitialData().count == 0)
      }

    }
  }
}
