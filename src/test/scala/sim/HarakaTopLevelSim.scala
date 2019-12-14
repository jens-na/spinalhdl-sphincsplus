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
object HarakaTopLevelSim {
  def main(args: Array[String]): Unit = {

    val rcList = SphincsPlusUtils.harakaRoundKeys(HARAKA_1024)
    var j = 0
    for(rc <- rcList) {
      println(s"RC[${j}] = ${rc.toString(16)}")
      j = j + 1
    }
    val clkConfig = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = HIGH, clockEdge = RISING)

    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new Haraka(new HarakaConfig())).doSim { dut =>
      dut.clockDomain.forkStimulus(10)
      SimClockCounter.count(dut.clockDomain, 10)

      dut.io.block #= 0
      dut.clockDomain.waitRisingEdge()
      dut.io.block #= 1

      for(j <- 0 until 100) {
        dut.clockDomain.waitRisingEdge()
//        println(dut.roundkeys(0))
//        println(dut.roundkeys(1))
      }

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }
  }
}