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

import spinal.core._

/**
 * The AES core component which performs KeyExpansion + AES Encode/Decode depending
 * on the set signals.
 */
class AES extends Component {
  val io = new Bundle {
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

  val aes_inst = new aes_core()
  io.encdec <> aes_inst.io.encdec
  io.init <> aes_inst.io.init
  io.xnext <> aes_inst.io.xnext
  io.ready <> aes_inst.io.ready
  io.key <> aes_inst.io.key
  io.keylen <> aes_inst.io.keylen
  io.xblock <> aes_inst.io.xblock
  io.result <> aes_inst.io.result
  io.result_valid <> aes_inst.io.result_valid
}

/**
 * The core AES encryption component which is used to perform AES rounds.
 * Round keys needs to be set by the caller. The default FIPS 197 S-Box is
 * used internally.
 */
class AESEnc extends Component {
  val io = new Bundle {
    val xnext = in Bool

    val keylen = in Bool
    val round = out UInt(4 bits)
    val round_key = in Bits(128 bits)

    val xblock = in Bits(128 bits)
    val new_block = out Bits(128 bits)
    val ready  = out Bool
  }

  val aes_encipher_inst = new aes_encipher_block()
  io.xnext <> aes_encipher_inst.io.xnext
  io.keylen <> aes_encipher_inst.io.keylen
  io.round <> aes_encipher_inst.io.round
  io.round_key <> aes_encipher_inst.io.round_key
  io.xblock <> aes_encipher_inst.io.xblock
  io.new_block <> aes_encipher_inst.io.new_block
  io.ready <> aes_encipher_inst.io.ready

  val aes_sbox = new aes_sbox()
  aes_encipher_inst.io.sboxw <> aes_sbox.io.sboxw
  aes_encipher_inst.io.new_sboxw <> aes_sbox.io.new_sboxw
}