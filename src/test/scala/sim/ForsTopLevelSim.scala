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
import sphincsplus.{Fors, ForsConfig, SphincsTopLevel}
import spinal.core._
import spinal.sim._
import spinal.core.sim._

import scala.util.Random

object ForsTopLevelSim {
  def main(args: Array[String]): Unit = {
    val forsCfg = ForsConfig()

    def sk_seed_fixed = List(0x53, 0xf, 0x8a, 0xfb, 0xc7, 0x45, 0x36, 0xb9, 0xa9, 0x63, 0xb4, 0xf1, 0xc4, 0xcb, 0x73, 0x8b)
    def pub_seed_fixed = List(0xdb, 0xcf, 0xe7, 0x68, 0x69, 0x1b, 0x5c, 0x5f, 0xec, 0x84, 0xa8, 0xd1, 0x25, 0xcb ,0x74, 0xed)
    def addr_fixed = List(0xfb8a0f53, 0xb93645c7, 0xf1b463a9, 0x8b73cbc4, 0x3d40a7ce, 0x6e6b604d, 0xd3c54e07, 0x189df3ba)
    def message_fixed= List(0x55, 0x7, 0x79, 0x5c, 0xff, 0x3b, 0xf, 0xc7, 0x15, 0xe3, 0xfe, 0x3b, 0xf1, 0xd4, 0x7d, 0xdb, 0xc6, 0x6d, 0x6f)
    def message_indices = List(0x755, 0x38f2, 0x6ffd, 0x3879, 0x315c, 0x7fdc, 0x3c4e, 0x3eea, 0x46db, 0x5edb)
    def pk_expected = List(0xb3, 0x6d, 0x6a, 0xd8, 0x6, 0xa, 0x44, 0x39, 0x9c, 0x7d, 0x25, 0x85, 0x46, 0x2c, 0x41, 0xe2)


    SimConfig.withWave.compile(new Fors(forsCfg)).doSim{ dut =>
      dut.clockDomain.forkStimulus(period = 10)
      SimClockCounter.count(dut.clockDomain, 10)

      dut.io.sign #= false
      dut.io.init #= true
      dut.clockDomain.waitRisingEdge()

      for(j <- 0 until 16) {
        dut.io.sk_seed(j) #= sk_seed_fixed(j)
        dut.io.pub_seed(j) #= pub_seed_fixed(j)
        dut.io.message(j) #= message_fixed(j)
      }
      dut.io.message(16) #= message_fixed(16)
      dut.io.message(17) #= message_fixed(17)
      dut.io.message(18) #= message_fixed(18)
      dut.io.init #= false
      dut.io.sign #= true

      // Simulate
      waitUntil(dut.io.ready.toBoolean == true)

      // Checking
      for(j <- 0 until forsCfg.forsTrees) {
        assert(
          assertion = dut.indices(j).toLong == message_indices(j),
          message =  s"Is: ${dut.indices(j).toLong.toHexString}, Should: ${message_indices(j).toLong.toHexString}"
        )
      }
      print("Simulation output message_indices: ")
      dut.indices.foreach(x => print(s"${x.toLong.toHexString} ")); println()

      // Print Output
      print("Simulation output o_pk: ")
      dut.io.o_pk.foreach(x => print(s"${x.toLong.toHexString} ")); println()

      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }
  }
}