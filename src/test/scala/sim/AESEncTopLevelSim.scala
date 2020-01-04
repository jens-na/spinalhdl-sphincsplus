///*
// * The MIT License (MIT)
// * Copyright (c) 2019, Jens Nazarenus
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
// * OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package sim
//
//
//import spinal.core._
//import spinal.core.sim._
//import sphincsplus.aes._
//import sphincsplus.utils._
//import spinal.sim.SimManagerContext
//
///**
// * AES Encryption test with aes_encipher_block. This test is neccessary because
// * Haraka uses its own KeyExpansion, hence own round keys.
// *
// * This is the same AES ECB Test as in AESTopLevelSim
// *
// * setKey(000102030405060708090a0b0c0d0e0f)
// * encryptAES(00112233445566778899aabbccddeeff)
// * R0 (Key = 000102030405060708090a0b0c0d0e0f)	 = 00102030405060708090a0b0c0d0e0f0
// *  R1 (Key = d6aa74fdd2af72fadaa678f1d6ab76fe)	 = 89d810e8855ace682d1843d8cb128fe4
// *  R2 (Key = b692cf0b643dbdf1be9bc5006830b3fe)	 = 4915598f55e5d7a0daca94fa1f0a63f7
// *  R3 (Key = b6ff744ed2c2c9bf6c590cbf0469bf41)	 = fa636a2825b339c940668a3157244d17
// *  R4 (Key = 47f7f7bc95353e03f96c32bcfd058dfd)	 = 247240236966b3fa6ed2753288425b6c
// *  R5 (Key = 3caaa3e8a99f9deb50f3af57adf622aa)	 = c81677bc9b7ac93b25027992b0261996
// *  R6 (Key = 5e390f7df7a69296a7553dc10aa31f6b)	 = c62fe109f75eedc3cc79395d84f9cf5d
// *  R7 (Key = 14f9701ae35fe28c440adf4d4ea9c026)	 = d1876c0f79c4300ab45594add66ff41f
// *  R8 (Key = 47438735a41c65b9e016baf4aebf7ad2)	 = fde3bad205e5d0d73547964ef1fe37f1
// *  R9 (Key = 549932d1f08557681093ed9cbe2c974e)	 = bd6e7c3df2b5779e0b61216e8b10b689
// *  R10 (Key = 13111d7fe3944a17f307a78b4d2b30c5)	 = 69c4e0d86a7b0430d8cdb78070b4c55a
// * = 69c4e0d86a7b0430d8cdb78070b4c55a
// *
// */
//object AESEncTopLevelSim {
//  def main(args: Array[String]): Unit = {
//
//    val plain0 = BigInt("00112233445566778899aabbccddeeff", 16)
//    val expected = BigInt("69c4e0d86a7b0430d8cdb78070b4c55a", 16)
//    val rc = List(
//      //BigInt("000102030405060708090a0b0c0d0e0f", 16), /* key */
//      BigInt("000102030405060708090a0b0c0d0e0f", 16), /* key */
//      BigInt("d6aa74fdd2af72fadaa678f1d6ab76fe", 16),
//      BigInt("b692cf0b643dbdf1be9bc5006830b3fe", 16),
//      BigInt("b6ff744ed2c2c9bf6c590cbf0469bf41", 16),
//      BigInt("47f7f7bc95353e03f96c32bcfd058dfd", 16),
//      BigInt("3caaa3e8a99f9deb50f3af57adf622aa", 16),
//      BigInt("5e390f7df7a69296a7553dc10aa31f6b", 16),
//      BigInt("14f9701ae35fe28c440adf4d4ea9c026", 16),
//      BigInt("47438735a41c65b9e016baf4aebf7ad2", 16),
//      BigInt("549932d1f08557681093ed9cbe2c974e", 16),
//      BigInt("13111d7fe3944a17f307a78b4d2b30c5", 16)
//    )
//    val clkConfig = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW, clockEdge = RISING)
//
//     /*
//     * AES Encipher
//     * Should work like __m128i _mm_aesenc_si128 (__m128i a, __m128i RoundKey)
//     */
//    SimConfig.withConfig(SpinalConfig(defaultConfigForClockDomains = clkConfig)).withWave.compile(new AESEnc).doSim { dut =>
//      dut.clockDomain.forkStimulus(period = 10)
//      SimClockCounter.count(dut.clockDomain, 10)
//
//      dut.io.keylen #= false
//      dut.io.raw #= false
//      dut.io.xblock #= plain0
//      dut.io.round_key #= rc(0)
//      dut.io.xnext #= true
//
//      dut.clockDomain.waitRisingEdge()
//      dut.io.xnext #= false
//      dut.clockDomain.waitRisingEdge()
//
//      // Perform AES rounds and print round/round key/result block
//      for(j <- 0 until 10) {
//        dut.io.round_key #= rc(j+1)
//        waitUntil(dut.io.round.toInt == j+2)
//        println(s"R${dut.io.round.toInt-1} ${dut.io.round_key.toBigInt.toString(16)} = ${dut.io.new_block.toBigInt.toString(16)}")
//      }
//
//      // Wait for last round to finish
//      waitUntil(dut.io.ready.toBoolean == true)
//
//      // Check final result
//      assert(
//        assertion = dut.io.new_block.toBigInt == expected,
//        message =  s"Is: ${dut.io.new_block.toBigInt.toString(16)}, Should: ${expected.toString(16)}"
//      )
//      println(s"Simulation clock cycles: ${SimClockCounter.pop()}")
//    }
//  }
//}