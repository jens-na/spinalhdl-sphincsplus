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
package sim

import sphincsplus.utils.SimClockCounter
import sphincsplus.{Fors, ForsConfig}
import spinal.core._
import spinal.core.sim._

object ForsTopLevelSim {
  def main(args: Array[String]): Unit = {
    val forsCfg = ForsConfig()

    def sk_seed = BigInt("530f8afbc74536b9a963b4f1c4cb738b", 16)
    def pub_seed = BigInt("dbcfe768691b5c5fec84a8d125cb74ed", 16)
    def addr_in = BigInt("fb8a0f53b93645c7f1b463a98b73cbc43d40a7ce6e6b604dd3c54e07189df3ba", 16)
    def m = BigInt("5507795cff3b0fc715e3fe3bf1d47ddbc66d6f", 16)
    SimConfig.withWave.compile(new Fors(forsCfg)).doSim{ dut =>
      dut.clockDomain.forkStimulus(period = 10)
      SimClockCounter.count(dut.clockDomain, 10)

      // Init
      dut.io.init #= true
      dut.io.xnext #= false
      dut.io.addr #= addr_in
      dut.io.message #= m
      dut.clockDomain.waitRisingEdge()

      // Perform Fors
      dut.io.addr  #= 0
      //dut.io.message #= 0
      dut.io.init #= false
      dut.io.xnext #= true
      dut.clockDomain.waitRisingEdge()
      dut.io.xnext #= false
      //      dut.clockDomain.waitRisingEdge()
      //      dut.clockDomain.waitRisingEdge()

      for(j <- 1 until 200) {
        dut.clockDomain.waitRisingEdge()
      }

      //waitUntil(dut.io.ready.toBoolean == true)

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }
  }
}