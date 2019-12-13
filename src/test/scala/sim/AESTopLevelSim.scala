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


import spinal.core._
import spinal.core.sim._
import sphincsplus.aes._
import sphincsplus.utils._
import spinal.sim.SimManagerContext

/**
 * Test vectors for FIPS 197: Advanced Encryption Standard (AES)
 * https://csrc.nist.gov/csrc/media/publications/fips/197/final/documents/fips-197.pdf (Appendix C)
 *
 * This simulation tests a ECB test vector to make sure the black box
 * for AES is connected correctly and is working as expected.
 *
 * setKey(000102030405060708090a0b0c0d0e0f)
 * encryptAES(00112233445566778899aabbccddeeff)
 * R0 (Key = 000102030405060708090a0b0c0d0e0f)	 = 00102030405060708090a0b0c0d0e0f0
 *  R1 (Key = d6aa74fdd2af72fadaa678f1d6ab76fe)	 = 89d810e8855ace682d1843d8cb128fe4
 *  R2 (Key = b692cf0b643dbdf1be9bc5006830b3fe)	 = 4915598f55e5d7a0daca94fa1f0a63f7
 *  R3 (Key = b6ff744ed2c2c9bf6c590cbf0469bf41)	 = fa636a2825b339c940668a3157244d17
 *  R4 (Key = 47f7f7bc95353e03f96c32bcfd058dfd)	 = 247240236966b3fa6ed2753288425b6c
 *  R5 (Key = 3caaa3e8a99f9deb50f3af57adf622aa)	 = c81677bc9b7ac93b25027992b0261996
 *  R6 (Key = 5e390f7df7a69296a7553dc10aa31f6b)	 = c62fe109f75eedc3cc79395d84f9cf5d
 *  R7 (Key = 14f9701ae35fe28c440adf4d4ea9c026)	 = d1876c0f79c4300ab45594add66ff41f
 *  R8 (Key = 47438735a41c65b9e016baf4aebf7ad2)	 = fde3bad205e5d0d73547964ef1fe37f1
 *  R9 (Key = 549932d1f08557681093ed9cbe2c974e)	 = bd6e7c3df2b5779e0b61216e8b10b689
 *  R10 (Key = 13111d7fe3944a17f307a78b4d2b30c5)	 = 69c4e0d86a7b0430d8cdb78070b4c55a
 * = 69c4e0d86a7b0430d8cdb78070b4c55a
 *
 * Simulation will be done for Nk=4 and Nr=10 (Because thats what Haraka is using too)
 */
object AESTopLevelSim {
  def main(args: Array[String]): Unit = {

    val plain = BigInt("00112233445566778899aabbccddeeff", 16)
    val aesKey = BigInt("000102030405060708090a0b0c0d0e0f00000000000000000000000000000000", 16) // 00000000000000000000000000000000
    val expected_output = BigInt("69c4e0d86a7b0430d8cdb78070b4c55a", 16)
    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)

    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new AES).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
      SimClockCounter.count(dut.clockDomain, 10)

      // Prepare KeyExpansion
      dut.io.init #= true
      dut.io.xnext #= false
      dut.io.key #= aesKey
      dut.io.keylen #= false
      dut.clockDomain.waitRisingEdge()
      dut.io.init #= false

      println("Waiting for: KeyExpansion")
      dut.clockDomain.waitRisingEdge()

      waitUntil(dut.io.ready.toBoolean == true)
      println("KeyExpansion: OK")

      // Prepare Encrypt
      dut.io.encdec #= true
      dut.io.xblock #= plain
      dut.io.xnext #= true
      dut.clockDomain.waitRisingEdge()
      dut.io.xnext #= false

      println("Waiting for: Encrypt")
      dut.clockDomain.waitRisingEdge()
      waitUntil(dut.io.ready.toBoolean == true)
      println("Encrypt: OK")

      print("Expected output io.result: ")
      println(expected_output.toString(16))

      print("Simulation output io.result: ")
      println(dut.io.result.toBigInt.toString(16))

      assert(
        assertion = dut.io.result.toBigInt == expected_output,
        message =  s"Is: ${dut.io.result.toBigInt}, Should: ${expected_output}"
      )

      println(s"Simulation clock cycles: ${SimClockCounter.cycles()}")
    }
  }
}