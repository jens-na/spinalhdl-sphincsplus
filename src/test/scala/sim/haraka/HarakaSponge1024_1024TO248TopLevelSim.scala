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
package sim.haraka

import sphincsplus.Params._
import sphincsplus._
import sphincsplus.utils._
import spinal.core._
import spinal.core.sim._

object HarakaSponge1024_1024TO248TopLevelSim {
  def main(args: Array[String]): Unit = {
    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)
    val input = BigInt("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
      "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
      "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f" +
      "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f",16)
    val expected = BigInt("", 16)

    def harakaCfg = new HarakaConfig(HARAKA_1024)
    def harakaSponge = new HarakaSpongeConstr(new HarakaSpongeConstrConfig(512, 256, 256, harakaCfg)) // Remainder = 8


    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(harakaSponge).doSim { dut =>
      dut.clockDomain.forkStimulus(10)
      SimClockCounter.count(dut.clockDomain, 10)

      // Init
      dut.io.init #= true
      dut.io.xnext #= false
      dut.io.xblock #= input
      dut.clockDomain.waitRisingEdge()

      // Perform Haraka
      dut.io.xblock #= 0
      dut.io.init #= false
      dut.io.xnext #= true
      dut.clockDomain.waitRisingEdge()
      dut.io.xnext #= false
//      dut.clockDomain.waitRisingEdge()
//      dut.clockDomain.waitRisingEdge()

      waitUntil(dut.io.ready.toBoolean == true)

//      for(j <- 0 until 500) {
//        dut.clockDomain.waitRisingEdge()
//      }

      assert(
        assertion = dut.io.result.toBigInt == expected,
        message =  s"Is: ${dut.io.result.toBigInt.toString(16)}, Should: ${expected.toString(16)}"
      )

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }

  }
}