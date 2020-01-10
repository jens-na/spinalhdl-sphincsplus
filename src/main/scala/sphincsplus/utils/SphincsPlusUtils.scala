package sphincsplus.utils

import sphincsplus.Params.{PI, _}
import spinal.core._

object SphincsPlusUtils {

  /**
   * Returns the Haraka round keys, based on the input bits. The keys are calculated by the decimals of PI.
   *
   * Haraka1024: 80 round keys
   * Haraka512: 40 round keys
   * Haraka256: 20 round keys
   *
   * @param harakaInput the input bits as Int value
   * @return the round keys as BigInt values. Each value holds a 128-bit key
   */
  def obtainHarakaRoundKeys(harakaInput: Int = 1024): List[BigInt] = {
    val rcList = List.range(0, harakaInput / 128 * 10)
    rcList.map(x => {
      var rc = BigInt(0)
      for(j <- 0 to 127 by 8) {
        val piChars = List(
          PI.charAt(128 * x + j).toInt & 1,
          PI.charAt(128 * x + j+1).toInt & 1,
          PI.charAt(128 * x + j+2).toInt & 1 ,
          PI.charAt(128 * x + j+3).toInt & 1,
          PI.charAt(128 * x + j+4).toInt & 1,
          PI.charAt(128 * x + j+5).toInt & 1,
          PI.charAt(128 * x + j+6).toInt & 1,
          PI.charAt(128 * x + j+7).toInt & 1
        )
        rc = rc << 4 | (piChars(7) << 3 | piChars(6) << 2 | piChars(5) << 1 | piChars(4) << 0)
        rc = rc << 4 | (piChars(3) << 3 | piChars(2) << 2 | piChars(1) << 1 | piChars(0) << 0)

      }
      //println(rc.toString(16))
      rc
    })
  }

  implicit class BitVectorExtension[T <: BitVector](val b: BitVector) {

    /**
     * Same as @see[[spinal.core.BitVector.subdivideIn(sliceCount: SlicesCount)]] but in a reverse order
     */
    def subdivideInRev(sliceCount: SlicesCount): Vec[T] = {
      require(b.getWidth % sliceCount.value == 0)
      val sliceWidth = b.getBitsWidth / sliceCount.value
      Vec((0 until sliceCount.value).reverseMap(i => b(i * sliceWidth, sliceWidth bits).asInstanceOf[T]))
    }

    /**
     * Same as @see[[spinal.core.BitVector.subdivideIn(sliceWidth: BitCount)]] but in a reverse order
     */
    def subdivideInRev(sliceWidth: BitCount): Vec[T] = {
      require(b.getWidth % sliceWidth.value == 0)
      subdivideInRev(b.getWidth / sliceWidth.value slices)
    }

    /**
     * Split the BitVector into slice of x bits and leave the remainder untouched
     * * @example {{{ val res = myBits.subdiviedIn(3 bits) }}}
     * @param sliceWidth the width of the slice
     * @return a Vector of slices
     */
    def subdivideInRemainder(sliceWidth: BitCount): Vec[T] = {
      val pow = Math.floor(Math.log10(b.getWidth)/Math.log10(2.0))
      val totalWidth = (Math.pow(2, pow)).toInt
      b.subdivideInRemainder(totalWidth / sliceWidth.value slices)
    }

    /**
     * Split the BitVector into x slice
     * @example {{{ val res = myBits.subdiviedIn(3 slices) }}}
     * @param sliceCount the width of the slice
     * @return a Vector of slices
     */
    def subdivideInRemainder(sliceCount: SlicesCount): Vec[T] = {
      val sliceWidth = b.getBitsWidth / sliceCount.value
      Vec((0 until sliceCount.value).reverseMap(i => b(i * sliceWidth, sliceWidth bits).asInstanceOf[T]))
    }
  }

}

