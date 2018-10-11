package com.johnsnowlabs.nlp

import org.apache.spark.ml.Model
import org.apache.spark.sql.{DataFrame, Dataset, Row}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.MetadataBuilder

/**
 * This trait implements logic that applies nlp using Spark ML Pipeline transformers
 * Should strongly change once UsedDefinedTypes are allowed
 * https://issues.apache.org/jira/browse/SPARK-7768
  */
abstract class AnnotatorModel[M <: Model[M]]
    extends RawAnnotator[M] {

  /**
    * internal types to show Rows as a relevant StructType
    * Should be deleted once Spark releases UserDefinedTypes to @developerAPI
    */
  private type AnnotationContent = Seq[Row]

  /**
    * takes a document and annotations and produces new annotations of this annotator's annotation type
    * @param annotations Annotations that correspond to inputAnnotationCols generated by previous annotators if any
    * @return any number of annotations processed for every input annotation. Not necessary one to one relationship
    */
  def annotate(annotations: Seq[Annotation]): Seq[Annotation]

  /**
    * Wraps annotate to happen inside SparkSQL user defined functions in order to act with [[org.apache.spark.sql.Column]]
    * @return udf function to be applied to [[inputCols]] using this annotator's annotate function as part of ML transformation
    */
  protected def dfAnnotate: UserDefinedFunction = udf {
    annotationProperties: Seq[AnnotationContent] =>
      annotate(annotationProperties.flatMap(_.map(Annotation(_))))
  }

  protected def beforeAnnotate(dataset: Dataset[_]): Dataset[_] = dataset

  protected def afterAnnotate(dataset: DataFrame): DataFrame = dataset

  /**
    * Given requirements are met, this applies ML transformation within a Pipeline or stand-alone
    * Output annotation will be generated as a new column, previous annotations are still available separately
    * metadata is built at schema level to record annotations structural information outside its content
    *
    * @param dataset [[Dataset[Row]]]
    * @return
    */
  override final def transform(dataset: Dataset[_]): DataFrame = {
    require(validate(dataset.schema), s"Wrong or missing inputCols annotators in $uid. " +
      s"Received inputCols: ${$(inputCols).mkString(",")}. Make sure such columns have following annotator types: " +
      s"${requiredAnnotatorTypes.mkString(", ")}")
    this match {
        // Preload embeddings once
      case withEmbeddings: ModelWithWordEmbeddings => withEmbeddings.embeddings
      case _ =>
    }

    val inputDataset = beforeAnnotate(dataset)

    val processedDataset = inputDataset.withColumn(
      getOutputCol,
      wrapColumnMetadata(dfAnnotate(
        array(getInputCols.map(c => dataset.col(c)):_*)
      ))
    )

    afterAnnotate(processedDataset)

  }

}
