package com.jsl.nlp.annotators

import com.jsl.nlp.{ContentProvider, DataBuilder}
import org.apache.spark.sql.{Dataset, Row}
import org.scalatest._

/**
  * Created by saif on 02/05/17.
  */
class StemmerTestSpec extends FlatSpec with StemmerBehaviors {

  val stemmer = new Stemmer
  "a Stemmer" should s"be of type ${Stemmer.annotatorType}" in {
    assert(stemmer.annotatorType == Stemmer.annotatorType)
  }

  val latinBodyData: Dataset[Row] = DataBuilder.basicDataBuild(ContentProvider.latinBody)

  "A full Stemmer pipeline with latin content" should behave like fullStemmerPipeline(latinBodyData)

}
