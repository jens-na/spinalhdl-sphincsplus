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
package sphincsplus

import spinal.core.sim._
import spinal.core._
import Params._

// Construction parameters for a Fors instance
case class Fors2Config(n : Int = 16,
                      forsTrees : Int = 10,
                      forsHeight : Int = 15) {
  val forsSigBytes = ((forsHeight + 1) * forsTrees * n)
  val forsMsgBytes = ((forsHeight * forsTrees + 7) / 8)
  val forsPkBytes = n

  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(
  //   assertion = n == 0,
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}

// Input/Output of a Fors instance
class Fors2Io(g: Fors2Config) extends Bundle {
  /* Input */
  val sk_seed, pub_seed = in Vec(Bits(8 bit), g.n)
  val message = in Vec(Bits(8 bits), g.forsMsgBytes)
  val sig = in Vec(Bits(8 bits), g.forsSigBytes)
  val in_addr = in Vec(Bits(32 bits), 8)

  /* Output */
  val o_sig = out Vec(Bits(8 bits), g.forsSigBytes)
  val o_pk = out Vec(Bits(8 bits), g.forsPkBytes)
  val o_addr = out Vec(Bits(32 bits), 8)

  /* Control parameter */
  val init = in Bool
  val sign = in Bool
  val ready = out Bool
}

/**
 * The FORS component is responsible for the two operations
 *
 * a) Signing the message with the secret key from sk_seed
 *    Req: Control parameter (sign == true)
 *    - Input: sk_seed, pub_seed, message, sign
 *    - Output: o_sig, o_pk
 *
 * b) Deriving a FORS public key from a provided signature
 *    Req: Control parameter (sign == false)
 *    - Input: sig, message, pub_seed, sign
 *    - Output: o_pk
 */
class Fors2(g: Fors2Config) extends Component {

  /**
   * Calculates the j-th message index
   * @param m the message at a specific position
   * @param j the index
   * @return the message index
   */
  def to_message_idx(m: UInt, j: UInt, offset: UInt) : UInt = {
    (((m >> (offset & 0x7)) & 0x1) << j.resize(8))
  }

  val io = new Fors2Io(g)
  val indices = Reg(Vec(Bits(32 bits), g.forsTrees)) simPublic()
  val roots = Reg(Vec(Vec(Bits(8 bits), g.n), g.forsTrees)) .keep() // FORS_TREES * FORS_N roots

  val Setup = new Area {
    val fire = io.init

    when(fire) {
      indices.map(_ := B"0000_0000".resize(32))
    }
  }

  // Interpret the message as g.forsHeight bit numbers.
  // Calculation in parallel, ready after g.forsTrees clock cycles
  val Indices = new Area {
    val fire = io.sign
    val curTree = Reg(UInt(log2Up(g.forsTrees) bits)) init(0) simPublic()
    val offset = curTree * g.forsHeight simPublic()
    val seqList = List.range(0, g.forsHeight)

    when(fire && curTree < g.forsTrees) {
      val idxList = seqList.map(x => to_message_idx(io.message(offset + x >> 3).asUInt, x, (offset + x)))
      indices(curTree) := (idxList.foldLeft(U(0))(_^_)).asBits.resize(32)
      curTree := curTree + 1
    }
    io.ready := curTree === g.forsTrees
  }

  // Generate a k*t FORS private key (leaf of the FORS tree) given an address and the provided sk_seed.
  val SecretKeyGen = new Area {

  }

  //
  for(j <- 0 until g.forsTrees) {
    for(k <- 0 until g.n) {
      roots(j)(k) := B"0".resize(8)
    }
  }

  io.o_addr(4) := B"0000_0000".resize(32)
  io.o_addr(0) := B"0000_0000".resize(32)
  io.o_addr(1) := B"0000_0000".resize(32)
  io.o_addr(2) := B"0000_0000".resize(32)
  io.o_addr(3) := B"0000_0000".resize(32)
  io.o_addr(5) := B"0000_0000".resize(32)
  io.o_addr(6) := B"0000_0000".resize(32)
  io.o_addr(7) := B"0000_0000".resize(32)

  io.o_sig.map(_ := B"0000_0000")
  io.o_pk.map(_ := B"0000_0000")

}