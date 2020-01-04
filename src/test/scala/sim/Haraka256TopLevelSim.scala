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


import sphincsplus.{Haraka, HarakaConfig}
import spinal.core._
import spinal.core.sim._
import sphincsplus.aes._
import sphincsplus.utils._
import spinal.sim.SimManagerContext
import sphincsplus.Params._

/**
 *
 */
object Haraka256TopLevelSim {
  def main(args: Array[String]): Unit = {
    val rcList = SphincsPlusUtils.obtainHarakaRoundKeys(HARAKA_1024)
    var j = 0
    for(rc <- rcList) {
      println(s"RC[${j}] = ${rc.toString(16)}")
      j = j + 1
    }
    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)
    val input256 = BigInt("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 16)
    val expected256 = BigInt("8026cebb7d4c714c70d95e54bb26f903794d381a37dec26ca20843f4a3363203", 16)

    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new Haraka(new HarakaConfig(HARAKA_256))).doSim { dut =>
      dut.clockDomain.forkStimulus(10)
      SimClockCounter.count(dut.clockDomain, 10)

      // Init
      dut.io.init #= true
      dut.io.xnext #= false
      dut.io.xblock #= input256
      dut.clockDomain.waitRisingEdge()

      // Perform Haraka
      dut.io.xblock #= 0
      dut.io.init #= false
      dut.io.xnext #= true
      dut.clockDomain.waitRisingEdge()
      dut.io.xnext #= false
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()

      waitUntil(dut.io.ready.toBoolean == true)
      assert(
        assertion = dut.io.result.toBigInt == expected256,
        message =  s"Is: ${dut.io.result.toBigInt.toString(16)}, Should: ${expected256.toString(16)}"
      )

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }


  }
}