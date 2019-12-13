package sphincsplus.aes

import spinal.core._
import sphincsplus.BuildInfo

/**
 * The component which controls the communication with the AES encoding blackbox.
 */
class aes_core extends BlackBox {

  val io = new Bundle {
    val clk = in Bool
    val reset_n = in Bool
    val encdec = in Bool
    val init = in Bool
    val xnext = in Bool
    val ready = out Bool

    val key = in Bits(256 bits)
    val keylen = in Bool // => 0=128 Bit, 1=256 Bit

    val xblock = in Bits(128 bits)
    val result = out Bits(128 bits)
    val result_valid = out Bool
  }

  // Map Clock
  mapCurrentClockDomain(clock=io.clk, reset=io.reset_n)

  // Don't prefix io_
  noIoPrefix()

  // Add RTL files
  val aesRTL = List[String](
    "aes_core.v",
    "aes_decipher_block.v",
    "aes_encipher_block.v",
    "aes_inv_sbox.v",
    "aes_key_mem.v",
    "aes_sbox.v"
  )
  aesRTL.foreach(file => addRTLPath(s"${BuildInfo.externalLibs}/aes/src/rtl/${file}"))
}