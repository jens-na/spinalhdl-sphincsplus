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
import sphincsplus.utils._
import sphincsplus.{Haraka, HarakaConfig}
import spinal.core._
import spinal.core.sim._

/**
 * Haraka512_S || 1024 -> 256
 */
object HarakaPerm1024TopLevelSim {
  def main(args: Array[String]): Unit = {
    val rcList = SphincsPlusUtils.obtainHarakaRoundKeys(HARAKA_1024)
    var j = 0
    for(rc <- rcList) {
      println(s"RC[${j}] = ${rc.toString(16)}")
      j = j + 1
    }
    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)
    val input = BigInt("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
                          "202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
                          "404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f" +
                          "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f",16)
    val expected = BigInt("c7cbf1d9dc9ed9e9be7f723b4e80a998add793d8870e2cc213b292287f306f625a6d57331cae5f34166f22b85b2b7cf3dd9277b0945be2aae6d7d715a68ab02d", 16)

    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new Haraka(new HarakaConfig(HARAKA_512))).doSim { dut =>
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

      waitUntil(dut.io.ready.toBoolean == true)
      assert(
        assertion = dut.io.result.toBigInt == expected,
        message =  s"Is: ${dut.io.result.toBigInt.toString(16)}, Should: ${expected.toString(16)}"
      )

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }

  }
}