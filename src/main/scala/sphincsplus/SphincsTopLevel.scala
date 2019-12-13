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

import spinal.core._

class SphincsTopLevel(forsCfg: ForsConfig) extends Component {
  val io = new Bundle {

  }
  val fors = new Fors(forsCfg)
  fors.io.sign := True
  fors.io.sk_seed.map(_ := B"0000_0000")
  fors.io.pub_seed.map(_ := B"0000_0000")
  fors.io.message.map(_ := B"0000_0000")
}

//object TopLevelSphincsPlus {
//  def main(args: Array[String]) {
//    SpinalVerilog(new SphincsTopLevel)
//  }
//}

//Define a custom SpinalHDL configuration with synchronous reset instead of the default asynchronous one. This configuration can be resued everywhere
object SphincsPlusConfig extends SpinalConfig(defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC))

//Generate the MyTopLevel's Verilog using the above custom configuration.
object TopLevelSphincsPlusWithCustomConfig {
  def main(args: Array[String]) {
    val forsCfg = ForsConfig()

    SphincsPlusConfig.generateVerilog(new SphincsTopLevel(forsCfg))
  }
}