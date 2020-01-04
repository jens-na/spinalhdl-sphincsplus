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
object Haraka512TopLevelSim {
  def main(args: Array[String]): Unit = {
    val rcList = SphincsPlusUtils.obtainHarakaRoundKeys(HARAKA_1024)
    var j = 0
    for(rc <- rcList) {
      println(s"RC[${j}] = ${rc.toString(16)}")
      j = j + 1
    }
    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)
    val input512 = BigInt("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f",16)
    val expected512= BigInt("c7caf3dad89bdfeeb6767830428da797bdc681cb931b3ad50bab8833632d717d7a4c7510388b79133e460893770652dceda34583a06ed49ddeeeed2e9ab78e12", 16)

    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new Haraka(new HarakaConfig(HARAKA_512))).doSim { dut =>
      dut.clockDomain.forkStimulus(10)
      SimClockCounter.count(dut.clockDomain, 10)

      // Init
      dut.io.init #= true
      dut.io.xnext #= false
      dut.io.xblock #= input512
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
      assert(
        assertion = dut.io.result.toBigInt == expected512,
        message =  s"Is: ${dut.io.result.toBigInt.toString(16)}, Should: ${expected512.toString(16)}"
      )

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }

  }
}