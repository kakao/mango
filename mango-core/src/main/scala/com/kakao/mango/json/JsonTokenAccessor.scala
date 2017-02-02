package com.kakao.mango.json

import com.kakao.shaded.jackson.core.JsonParser

import scala.util.Try

/** Enables a [[JsonParser]] to be used with pattern matching */
case class JsonTokenAccessor(parser: JsonParser) extends AnyVal {
  def getBigIntegerValue = parser.getBigIntegerValue
  def getBinaryValue = parser.getBinaryValue
  def getBooleanValue = parser.getBooleanValue
  def getByteValue = parser.getByteValue
  def getCurrentName = parser.getCurrentName
  def getDecimalValue = parser.getDecimalValue
  def getDoubleValue = parser.getDoubleValue
  def getFloatValue = parser.getFloatValue
  def getIntValue = parser.getIntValue
  def getLongValue = parser.getLongValue
  def getNumberType = parser.getNumberType
  def getNumberValue = parser.getNumberValue
  def getShortValue = parser.getShortValue
  def getText = parser.getText
  def getTextCharacters = parser.getTextCharacters
  def getTextLength = parser.getTextLength
  def getTextOffset = parser.getTextOffset
  def getValueAsBoolean = parser.getValueAsBoolean
  def getValueAsDouble = parser.getValueAsDouble
  def getValueAsInt = parser.getValueAsInt
  def getValueAsLong = parser.getValueAsLong
  def getValueAsString = parser.getValueAsString
}

object StringField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, String)] = {
    Option((accessor.getCurrentName, accessor.getValueAsString)).collect {
      case (name: String, value: String) => (name, value)
    }
  }
}

object NumberField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, Number)] = {
    Try((accessor.getCurrentName, accessor.getNumberValue)).toOption.collect {
      case (name: String, value: Number) => (name, value)
    }
  }
}

object IntField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, Int)] = {
    Try((accessor.getCurrentName, accessor.getIntValue)).toOption.collect {
      case (name: String, value: Int) => (name, value)
    }
  }
}

object LongField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, Long)] = {
    Try((accessor.getCurrentName, accessor.getLongValue)).toOption.collect {
      case (name: String, value: Long) => (name, value)
    }
  }
}

object DoubleField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, Double)] = {
    Try((accessor.getCurrentName, accessor.getDoubleValue)).toOption.collect {
      case (name: String, value: Double) => (name, value)
    }
  }
}

object BooleanField {
  def unapply(accessor: JsonTokenAccessor): Option[(String, Boolean)] = {
    Try((accessor.getCurrentName, accessor.getBooleanValue)).toOption.collect {
      case (name: String, value: Boolean) => (name, value)
    }
  }
}
