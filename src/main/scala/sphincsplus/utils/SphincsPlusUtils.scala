package sphincsplus.utils

import sphincsplus.Params._

object SphincsPlusUtils {

  /**
   * Returns the Haraka round keys, based on the input bits.
   * The keys are calculated by the decimals of PI.
   *
   * Haraka1024: 80 round keys
   * Haraka512: 40 round keys
   * Haraka256: 20 round keys
   *
   * @param harakaInput the input bits
   * @return the round keys as BigInt values
   */
  def harakaRoundKeys(harakaInput: Int = 1024): List[BigInt] = {
    val rcList = List.range(0, harakaInput / 128 * 10)
    rcList.map(x => {
      var rc = BigInt(0)
      for(j <- 127 to 0 by -4) {
        val piChars = List(
          PI.charAt(128 * x + j).toInt & 1,
          PI.charAt(128 * x + j-1).toInt & 1,
          PI.charAt(128 * x + j-2).toInt & 1 ,
          PI.charAt(128 * x + j-3).toInt & 1
        )
        rc = rc << 4 | (piChars(0) << 3 | piChars(1) << 2 | piChars(2) << 1 | piChars(3) << 0)
      }
      rc
    })
  }

}
