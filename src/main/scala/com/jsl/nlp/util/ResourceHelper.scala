package com.jsl.nlp.util

import java.io.{FileNotFoundException, InputStream}

import com.jsl.nlp.annotators.pos.{TaggedSentence, TaggedWord}
import com.jsl.nlp.util.regex.RegexRule

import scala.io.Source
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map => MMap}

/**
  * Created by saif on 28/04/17.
  */
object ResourceHelper {

  private case class SourceStream(resource: String) {
    val pipe: InputStream = try {
      getClass.getResourceAsStream("/" + resource)
    } catch {
      case _: Throwable =>
        throw new FileNotFoundException(s"Lemma dictionary $resource not found")
    }
    val content: Source = Source.fromInputStream(pipe)("UTF-8")
  }

  private def wordTagSplitter(sentence: String, tagSeparator: Char):
  Array[TaggedWord] = {
    val taggedWords: ArrayBuffer[TaggedWord] = ArrayBuffer()
      sentence.split("\\s+").foreach { token => {
        val tagSplit: Array[String] = token.split('|').filter(_.nonEmpty)
        if (tagSplit.length == 2) {
          val word = tagSplit(0)
          val tag = tagSplit(1)
          taggedWords.append(TaggedWord(word, tag))
        }
      }}
    taggedWords.toArray
  }

  def loadRules: Array[RegexRule] = {
    ???
  }

  /**
    * Standard key value parser from source
    *
    * @param source File input to streamline
    * @param format format, for now only txt
    * @param keySep separator character
    * @param valueSep values separator in dictionary
    * @return
    */
  def parseKeyValuesText(
                         source: String,
                         format: String,
                         keySep: String,
                         valueSep: String): Map[String, Array[String]] = {
    format match {
      case "txt" =>
        val sourceStream = SourceStream(source)
        val res = sourceStream.content.getLines.map (line => {
          val kv = line.split (keySep).map (_.trim)
          val key = kv (0)
          val values = kv (1).split (valueSep).map (_.trim)
          (key, values)
        }).toMap
        sourceStream.pipe.close()
        res
    }
  }

  /**
    * Specific approach chosen is to generate quick reads of Lemma Dictionary
    *
    * @param source File input to streamline
    * @param format format, for now only txt
    * @param keySep separator cha
    * @param valueSep values separator in dictionary
    * @return
    */
  def flattenRevertValuesAsKeys(
                                 source: String,
                                 format: String,
                                 keySep: String,
                                 valueSep: String): Map[String, String] = {
    format match {
      case "txt" => {
        val m: MMap[String, String] = MMap()
        val sourceStream = SourceStream(source)
        sourceStream.content.getLines.foreach(line => {
          val kv = line.split(keySep).map(_.trim)
          val key = kv(0)
          val values = kv(1).split(valueSep).map(_.trim)
          values.foreach(m(_) = key)
        })
        sourceStream.pipe.close()
        m.toMap
      }
      case _ => throw new IllegalArgumentException("Only txt supported as a file format")
    }
  }

  def parsePOSCorpusFromText(
                              text: String,
                              tagSeparator: Char
                            ): Array[TaggedSentence] = {
    val sentences: ArrayBuffer[Array[TaggedWord]] = ArrayBuffer()
    text.split("\n").filter(_.nonEmpty).foreach{sentence =>
      sentences.append(wordTagSplitter(sentence, tagSeparator))
    }
    sentences.map(TaggedSentence).toArray
  }

  def parsePOSCorpusFromSource(
                  source: String,
                  tagSeparator: Char
                ): Array[TaggedSentence] = {
    val sourceStream = SourceStream(source)
    val lines = try {
      sourceStream.content.getLines()
        .filter(_.nonEmpty)
        .map(sentence => wordTagSplitter(sentence, tagSeparator))
        .toArray
    } catch {
      case _: java.nio.charset.UnmappableCharacterException =>
        throw new Exception(s"file $source contains dirty characters")
    }
    sourceStream.pipe.close()
    lines.map(TaggedSentence)
  }

  def parsePOSCorpusFromSources(sources: List[String], tagSeparator: Char): Array[TaggedSentence] = {
    sources.flatMap(parsePOSCorpusFromSource(_, tagSeparator)).toArray
  }

}
