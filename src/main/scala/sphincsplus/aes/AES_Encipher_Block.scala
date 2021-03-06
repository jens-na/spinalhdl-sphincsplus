/*
 * The MIT License (MIT)
 * Copyright (c) 2019, Jens Nazarenus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package sphincsplus.aes

import sphincsplus.BuildInfo
import spinal.core._

/**
 * The component which controls the communication with the AES encoding blackbox.
 */
class aes_encipher_block extends BlackBox {

  val io = new Bundle {
    val clk = in Bool
    val reset_n = in Bool

    val xnext = in Bool
    val raw = in Bool

    val keylen = in Bool
    val round = out UInt(4 bits)
    val round_key = in Bits(128 bits)

    val sboxw = out Bits(32 bits)
    val new_sboxw = in Bits(32 bits)

    val xblock = in Bits(128 bits)
    val new_block = out Bits(128 bits)
    val ready  = out Bool
  }

  // Map Clock
  mapCurrentClockDomain(clock=io.clk, reset=io.reset_n)

  // Don't prefix io_
  noIoPrefix()

  // Add RTL files
  val aesRTL = List[String](
    "aes_encipher_block.v",
    "aes_key_mem.v",
    "aes_sbox.v"
  )
  aesRTL.foreach(file => addRTLPath(s"${BuildInfo.externalLibs}/aes/src/rtl/${file}"))
}