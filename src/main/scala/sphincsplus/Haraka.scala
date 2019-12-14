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
import sphincsplus.utils._
import Params._

// Construction parameters for a Haraka instance
case class HarakaConfig(lenInput : Int = 1024, lenOutput : Int = 256) {
  val roundKeys = lenInput / 128 * 10
  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(x
  //   assertion = n == 0,1
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}

class HarakaIo(g: HarakaConfig) extends Bundle {
  val block = in Bits(g.lenInput bits)
  val result = out Bits(g.lenOutput bits)

  val ready = out Bool
}

class Haraka(g: HarakaConfig) extends Component {
  val io = new HarakaIo(g)

  val test = Reg(Bits(128 bits)) .keep()
  val roundkeys = Mem(Bits(128 bits), SphincsPlusUtils.harakaRoundKeys(g.lenInput).map(x => B(x, 128 bits)))

  when(io.block =/= 0) {
    io.ready := False
    test := roundkeys("0000001")
  }.otherwise {
    io.ready := True
    test := roundkeys("0000000")
  }
  io.result := 0
}
