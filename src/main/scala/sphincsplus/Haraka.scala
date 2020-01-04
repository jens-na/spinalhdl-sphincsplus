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

import spinal.lib.{Counter, Delay}
import spinal.core._
import sphincsplus.utils._
import sphincsplus.aes.AESEnc128
import sphincsplus.utils.SphincsPlusUtils.BitVectorExtension

/**
 * Construction parameters for a Haraka instance
 * @param lenInput the input length of Haraka (1024, 512, 256)
 * @param aesInstances the number of AES cores used for parallel optimizations
 */
case class HarakaConfig(lenInput : Int = 1024, aesInstances : Int = 1) {
  val lenOutput = lenInput // We don't truncate anything in this implementation, we just output the states after the final round
  val blocks = lenInput / 128 // Number of 128-bit blocks of the state (Haraka parameter b)
  val roundKeys = blocks * 10 // Number of round keys that are needed for the calculation
  val aesRounds = 5 // Number of rounds (Haraka parameter T)
  val aesOperations = 2 // Number of AES operations (Haraka parameter m)

  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(x
  //   assertion = n == 0,1
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}

class HarakaIo(g: HarakaConfig) extends Bundle {
  val xblock = in Bits(g.lenInput bits)
  val result = out Bits(g.lenOutput bits)
  val init = in Bool
  val xnext = in Bool
  val ready = out Bool
}

class Haraka(g: HarakaConfig) extends Component {
  val io = new HarakaIo(g)

  // Fire values
  val busy = Reg(Bool) init(False)
  val busyInternal = RegNext(busy) init(False)
  val clearAllCounter = RegNext(False) init(False)

  // The round keys to use with AES. Depending on the Haraka mode this
  // Mem is filled with 20, 40 or 80 round keys
  val roundkeys = Mem(Bits(128 bits), SphincsPlusUtils.obtainHarakaRoundKeys(g.lenInput).map(x => B(x, 128 bits)))
  val roundkeysSelector = Reg(UInt(log2Up(g.roundKeys) bits)) init(0)

  // The state holds the input divided into 128 bit values, which is necessary for
  // the AES rounds.
  val state = Reg(Vec(Bits(128 bits), g.blocks))
  val state0 = Reg(Bits(128 bits))
  state0 := state(0)

  // The AES component connection
  val aes = new AESEnc128()

  // Based on the Haraka mode this componenent need different cylces for the mixing
  // process.
  val mixingCycles = g.lenInput match {
    case 1024 => 3
    case 512 => 2
    case 256 => 1
  }
  val aesEncodeCycles = Counter(2)
  val aesStateCounter = Counter(g.blocks)
  val mixing = Reg(Bool()) init(False)
  val mixingCounter = Counter(mixingCycles)

  val aesRounds = Counter(g.aesRounds)
  val aesOps = Counter(g.aesOperations)
  val aesEncodeFinsished = Reg(Bool) init(False)
  val aesState = aesStateCounter.valueNext

  // AES Encode process is finished every two cycles when the component is busy
  // An AES Encode process can not be finished when the mixing process is running
  aesEncodeFinsished := (aesEncodeCycles % 2 === 0) && busyInternal && !mixing

  val HarakaInit = new Area {
    // When init has a high value, all input data is attached.
    // The Haraka componenent will take this and place them in a register for the
    // internal states
    when(io.init) {
      state := io.xblock.subdivideInRev(128 bits)
      aesRounds.clear()
      busy := True
    }

    // HarakaNext is set, when io.xnext gets a high value. After that the caller has to
    // wait for the ready signal, which signals that the Haraka component is finished
    // with the calculation of the hash.
    when(io.xnext) {
      busy := True
    }
  }

  val HarakaAES = new Area {
    // When Haraka is busy reset the counter and start counting the AES Rounds
    when(!busyInternal) {
      aesEncodeCycles.clear()
      aesStateCounter.clear()
    }

    // When all States went through AES we increment the AES operation counter
    when(aesStateCounter.willOverflow) {
      aesOps.increment()
    }

    // When one round of AES is finished, increment the state counter, to calculate
    // the next state and also set the state value of the returned out value of the
    // AES component
    when(aesEncodeFinsished & !mixing) {
      aesStateCounter.increment()
      state(aesState - 1) := aes.io.state_out
      roundkeysSelector := roundkeysSelector + 1
    }

    // Perform AES when Haraka is not mixing
    when(busy && !mixing) {
      aesEncodeCycles.increment()
    }

    // State and Key selection based on the Counter
    aes.io.state_in := state(aesState)
    aes.io.key := roundkeys(roundkeysSelector)
  }

  val HarakaMixing = new Area {

    // equiv _mm_unpacklo_epi32
    def unpacklo(x: Bits, y: Bits) : Bits = {
      B(128 bits,
        (31 downto 0) -> y(31 downto 0),    (63 downto 32) -> x(31 downto 0),
        (95 downto 64) -> y(63 downto 32),  (127 downto 96) -> x(63 downto 32)
      )
    }

    // equiv _mm_unpackhi_epi32
    def unpackhi(x: Bits, y: Bits) : Bits = {
      B(128 bits,
        (31 downto 0) ->  y(95 downto 64),  (63 downto 32) -> x(95 downto 64),
        (95 downto 64) -> y(127 downto 96), (127 downto 96) -> x(127 downto 96)
      )
    }

    // Permutation after the g.aesOperations have been performed.
    // Set mixing to high, so no new AES values get calculated until mixing
    // is done.
    when(aesOps.willOverflow) {
      mixing := True
    }

    when(mixing) {
      val mixing512Temp = Reg(Bits(128 bits))

      // Haraka 256 mixing
      if(g.lenInput == Params.HARAKA_256) {
        state(1) := unpacklo(state0, state(1))
        state(0) := unpackhi(state0, state(1))
      }

      // Haraka 512 mixing
      if(g.lenInput == Params.HARAKA_512) {
        when(mixingCounter === 0) { // Cycle 1
          mixing512Temp := unpackhi(state0, state(1))
          state(0) := unpacklo(state0, state(1))
          state(1) := unpackhi(state(2), state(3))
          state(2) := unpacklo(state(2), state(3))
          mixingCounter.increment()
        }
        when(mixingCounter === 1) { // Cycle 2
          state(3) := unpackhi(state(0), state(2))
          state(0) := unpacklo(state(0), state(2))
          state(2) := unpacklo(state(1), mixing512Temp)
          state(1) := unpackhi(state(1), mixing512Temp)
        }

      }

      // Reset after mixing is done for the next AES round
      when(mixingCounter.willOverflowIfInc) {
        busyInternal := False
        aesOps.clear()
        aesEncodeCycles.clear()
        aesStateCounter.clear()
        mixing := False
        aesRounds.increment()
        mixingCounter.clear()
      }
    }

    when(aesRounds.willOverflow) {
      busy := False
    }
  }

  // Result output
  // Caller can use the busy signal to wait for a new result, if init has been used to set a new input
  // value and next to start the calculation.
  io.result := Cat(state.reverse)
  io.ready := !busy
}
