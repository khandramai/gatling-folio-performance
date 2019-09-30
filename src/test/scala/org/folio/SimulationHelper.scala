package org.folio

object SimulationHelper {

  /**
    * This method generates random alphanumerical string of fixed size
    * @param length length of random poNumber
    * @return random alphanumerical string
    */
  def getRandomAlphaNumericString(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    val sb = new StringBuilder
    for (_ <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

}
