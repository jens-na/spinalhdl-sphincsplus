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

/**
 * AES Encryption test with aes_encipher_block. This test is necessary because
 * Haraka uses its own KeyExpansion
 *
 * = input bytes =
 * 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
 *
 * = AES Rounds =
 * f7 11 dd 30 dc 93 f6 e3 ba 19 7d 87 6b ec a5 5a
 * aa 1f b1 42 6e f6 bc 32 e1 7e e2 62 fd 01 8f a4
 * ba b7 ca 1e be 54 25 8e 6d 1f ec 07 cd a9 f6 bd
 * 27 87 dd 1d 2c 81 7c 8f 7b 04 2c d1 7b 70 3e db
 * 5f fe f2 5b f9 e7 0b 80 03 a8 7c 15 55 01 54 18
 * 9f 61 97 8f 6d 5f 50 20 36 ce 42 f9 47 b5 fa 3d
 * ec 0d 25 db a0 35 78 04 51 80 19 27 bb 09 59 6e
 * 6d 01 51 2e e5 9e 70 54 77 60 a3 a9 dd 4c 11 cd
 * d2 89 ff 24 71 ce 45 40 7b ef 05 bc 9f 19 71 c8
 * 74 ed cb ef b2 ce 7b ee 3a a1 c2 b3 54 59 0f 75
 *
 */
object AESRoundTopLevelSim {
  def main(args: Array[String]): Unit = {

    val plain0 = BigInt("000102030405060708090a0b0c0d0e0f", 16)

    val rc = List(
      BigInt("9d7b8175f0fec5b20ac020e64c708406", 16),
      BigInt("17f7082fa46b0f646ba0f388e1b4668b", 16),
      BigInt("1491029f609d02cf9884f2532dde0234", 16),
      BigInt("794f5bfdafbcf3bb084f7b2ee6ead60e", 16),
      BigInt("447039be1ccdee798b447248cbb0cfcb", 16),
      BigInt("7b058a2bed35538db732906eeecdea7e", 16),
      BigInt("1bef4fda612741e2d07c2e5e438fc267", 16),
      BigInt("3b0bc71fe2fd5f6707cccaafb0d92429", 16),
      BigInt("ee65d4b9ca8fdbece97f86e6f1634dab", 16),
      BigInt("337e03ad4f402a5b64cdb7d484bf301c", 16)
    )

    val expected = List(
      BigInt("f711dd30dc93f6e3ba197d876beca55a", 16),
      BigInt("f99880d3d6505d2f019a63a2d70526e4", 16),
      BigInt("5ad19e55d564134d05775b5b8db29c58", 16),
      BigInt("88566a8a8fcbbd766fb40f725a7504d8", 16),
      BigInt("e171ad42facc6d6f46f30b445eb9f459", 16),
      BigInt("addd29d533f46210c4c84e088797443f", 16),
      BigInt("aa1ba404c70159490455aed8afd40b55", 16),
      BigInt("e494db8bb3573e1e524eafd502c2dc04", 16),
      BigInt("5ac319dfda10c9884d975c9ec89e62a4", 16),
      BigInt("12d83866566703a7b49bbba6eb7c4ada", 16)
    )

    val clkConfig = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = HIGH, clockEdge = RISING)

    /*
    * AES Encipher
    * Should work like __m128i _mm_aesenc_si128 (__m128i a, __m128i RoundKey)
    */
    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new AESEnc128).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)
      SimClockCounter.count(dut.clockDomain, 10)

      dut.io.state_in #= plain0
      dut.io.key #=  rc(0)
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()

      for(j <- 1 until 10) {
        dut.io.state_in := dut.io.state_out
        dut.io.key #=  rc(j)
        dut.clockDomain.waitRisingEdge()
        dut.clockDomain.waitRisingEdge()
      }

      // Wait for last round to finish
      //waitUntil(dut.io.ready.toBoolean == true)

//      // Check final result
//      assert(
//        assertion = dut.io.new_block.toBigInt == expected,
//        message =  s"Is: ${dut.io.new_block.toBigInt.toString(16)}, Should: ${expected.toString(16)}"
//      )
      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
    }
  }
}