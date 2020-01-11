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

import scala.reflect.internal.util.NoPosition


/**
 * Construction parameters for a Haraka instance
 * @param lenInput the input length of Haraka (1024, 512, 256)
 */
class HarakaConfig(val lenInput : Int = 256) {
  val lenOutput = lenInput // We don't truncate anything in this component
  val blocks = lenInput / 128 // Number of 128-bit blocks of the state (Haraka parameter b)
  val roundKeys = blocks * 10 // Number of round keys that are needed for the calculation
  val aesRounds = 5 // Number of rounds (Haraka parameter T)
  val aesOperations = 2 // Number of AES operations (Haraka parameter m)

  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(
  //   assertion = n == 0,1
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}

/**
 * The Haraka input/output signals
 * @param g the Haraka configuration parameters
 */
class HarakaIo(g: HarakaConfig) extends Bundle {
  val xblock = in Bits(g.lenInput bits)
  val result = out Bits(g.lenOutput bits)
  val init = in Bool
  val xnext = in Bool
  val ready = out Bool
}

/**
 * The Haraka componenent which does the calculation
 * @param g the Haraka configuration parameters
 */
class Haraka(g: HarakaConfig) extends Component {
  val io = new HarakaIo(g)

  // Fire values
  val busy = Reg(Bool) init(False)
  val busyInternal = RegNext(busy) init(False)

  // The round keys to use with AES. Depending on the Haraka mode this
  // Mem is filled with 20, 40 or 80 round keys
  val roundkeys = Mem(Bits(128 bits), SphincsPlusUtils.obtainHarakaRoundKeys(g.lenInput).map(x => B(x, 128 bits)))
  val roundkeysSelector = Reg(UInt(log2Up(g.roundKeys) bits)) init(0)

  // The state holds the input divided into 128 bit values, which is necessary for
  // the AES rounds.
  val state = Reg(Vec(Bits(128 bits), g.blocks))
  val input = Reg(Vec(Bits(128 bits), g.blocks))
  val state0 = Reg(Bits(128 bits))
  state0 := state(0)

  // The AES component connection
  val aes = new AESEnc128()

  // Based on the Haraka mode this component need different cycles for the mixing
  // process.
  val mixingCycles = g.lenInput match {
    case 1024 => 3
    case 512 => 2
    case 256 => 1
  }
  val aesEncodeCycles = Counter(2)
  val aesStateCounter = Counter(g.blocks)
  val mixing = Reg(Bool()) init(False)
  val dm = Reg(Bool()) init(False)
  val mixingCounter = Counter(mixingCycles)

  val aesRounds = Counter(g.aesRounds)
  val aesOps = Counter(g.aesOperations)
  val aesEncodeFinsished = Reg(Bool) init(False)
  val aesState = aesStateCounter.valueNext

  // AES Encode process is finished every two cycles when the component is busy
  // An AES Encode process can not be finished when the mixing process is running
  aesEncodeFinsished := (aesEncodeCycles % 2 === 0) && busyInternal && !mixing

  val Init = new Area {
    // When init has a high value, all input data is attached.
    // The Haraka componenent will take this and place them in a register for the
    // internal states
    when(io.init) {
      input := io.xblock.subdivideInRev(128 bits)
      state := io.xblock.subdivideInRev(128 bits)
      aesRounds.clear()
      busy := True
    }

    // HarakaNext is set, when io.xnext gets a high value. After that the caller has to
    // wait for the ready signal, which signals that the Haraka component is finished
    // with the calculation of the hash.
    when(io.xnext) {
      busy := True
      roundkeysSelector := 0
    }
  }

  val AES = new Area {
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

    // Reset the roundKeySelector to 0

    // Perform AES when Haraka is not mixing
    when(busy && !mixing) {
      aesEncodeCycles.increment()
    }

    // State and Key selection based on the Counter
    aes.io.state_in := state(aesState)
    aes.io.key := roundkeys(roundkeysSelector)

  }

  val Mixing = new Area {

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

    // AES rounds are finished
    when(aesRounds.willOverflow) {
      dm := True
    }
  }

  val DaviesMeyer = new Area {
    // XOR input to states (DM-effect)
    when(dm) {
      (state, input).zipped.map({ (s, i) => s := s ^ i })
      dm := False
      busy := False
    }
  }

  // Result output
  // Caller can use the busy signal to wait for a new result, if init has been used to set a new input
  // value and next to start the calculation.
  io.result := Cat(state.reverse)
  io.ready := !busy
}

/**
 * Definition of a Haraka sponge construction
 *
 * @param bitWidth the bitWidth aka the inputLength (Parameter b)
 * @param outputLen the desired output length
 * @param capacity the capacity (Parameter c)
 */
class HarakaSpongeConstrConfig(val bitWidth: Int, val outputLen: Int, val capacity: Int = 256, val harakaCfg: HarakaConfig) {
  // Determine the width of the blocks
  val powerOfBlocks = Math.floor(Math.log10(bitWidth)/Math.log10(2.0)) // 2^x
  val blockTotalWidth = (Math.pow(2, powerOfBlocks)).toInt
  val bitrate = (blockTotalWidth - capacity).toInt // 512 - 256

  // If bitWidth is not a power of 2 there is a remainder of y bits that has to be hashed too.
  // If 0, there is no remainder
  val remainderWidth = (bitWidth - blockTotalWidth).toInt
  val remainderCount = remainderWidth / 8

  println(s"bitWidth: ${bitWidth}, outputLen: ${outputLen}, bitrate: ${bitrate}")
  println(s"remainderWidth: ${remainderWidth}, blockTotalWidth: ${blockTotalWidth}, powerOfBlocks: ${powerOfBlocks}, bitWidth: ${bitWidth}")

  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(
  //   assertion = n == 0,1
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}

/**
 * The Sponge construction of Haraka input/output signals
 * @param c the Haraka sponge construction parameters
 */
class HarakaSpongeIo(c: HarakaSpongeConstrConfig) extends Bundle {
  val xblock = in Bits(c.bitWidth bits)
  val result = out Bits(c.outputLen bits)
  val init = in Bool
  val xnext = in Bool
  val ready = out Bool
}

/**
 * The sponge construction for the Haraka componenent
*/
class HarakaSpongeConstr(c: HarakaSpongeConstrConfig) extends Component {
  val io = new HarakaSpongeIo(c)

  // Range selectors
  val mainBlock = (c.blockTotalWidth -1 downto 0)
  val remainderBlock = (c.bitWidth -1 downto c.blockTotalWidth)
  val xorRangeSelector = (c.blockTotalWidth -1 downto c.blockTotalWidth - c.bitrate) // 511 downto 256 i.e.

  // Based on the parameters we need to do 1..(c.blockTotalWidth / c.bitrate) absorb operations
  val absorbOperations = (c.blockTotalWidth / c.bitrate)
  val absorbCounter = Counter(absorbOperations + 1)

  // Based on the output length we need to do 1..(c.outputLen / c.bitrate) squeeze operations
  val squeezeOperations = (c.outputLen / 256) // OK ? Was (c.outputLen / c.bitrate) before but when 256 / 768, nothing gets squeezed
  val squeezeCounter = Counter(squeezeOperations) // + 1 for convenience to make use of counter.willOverflow

  // Fire values
  val absorbBusy = Reg(Bool) init(False)
  val squeezeBusy = Reg(Bool) init(False)
  val busy = absorbBusy || squeezeBusy
  val remainderCtrl = Reg(Bool) init(False)

  // Haraka control
  val harakaInit = Reg(Bool) init(False)
  val harakaBusy = Reg(Bool) init(False)
  val harakaStart = Reg(Bool) init(False)
  val xorInput = Reg(Bool) init(False)

  // State+Remainder/Input
  val input = Reg(Vec(Bits(c.bitrate bits), absorbOperations))
  val state = Reg(Bits(c.blockTotalWidth bits)) init(0)
  val remainder = Reg(Vec(Bits(8 bits), c.bitrate/8)) // 8 * 32 = 256 bits i.e.

  // Haraka component
  val haraka = new Haraka(new HarakaConfig(c.harakaCfg.lenInput)) // Haraka based on width
  haraka.io.xblock := 0
  haraka.io.init := False
  haraka.io.xnext := False

  val Init = new Area {
    when(io.init) {

      input := io.xblock(c.bitWidth - 1 downto c.remainderWidth).subdivideIn(c.bitrate bits)

      if(c.remainderCount == 1) {
        remainder(0) :=  U(0x1f, 8 bits).asBits
      }
      if(c.remainderCount > 1) {
        for(j <- 0 until c.remainderCount - 1) {
          val from = (c.remainderWidth - 1) - (j * 8)
          val to = (c.remainderWidth) - (j*8) - 8
          remainder(j) := io.xblock(from downto to)
        }
        remainder(c.remainderCount -1) :=  U(0x1f, 8 bits).asBits
      }

      // If remainderCount == 0, the remainder will be filled with low values.
      remainder(c.bitrate / 8 - 1) := 128
      List.range(c.remainderCount, c.bitrate / 8 - 1).map(x => remainder(x) := B"0000_0000")

      absorbCounter.clear()
      squeezeCounter.clear()
      absorbBusy := True
      xorInput := True
    }
  }

  val Xor = new Area {
    // XOR input for every absorbation round except the last one
    when(xorInput) {
      state(xorRangeSelector) := state(xorRangeSelector) ^ input((absorbCounter-1).resize(log2Up(input.getBitsWidth))) // Remainder?
      absorbCounter.increment()
      harakaInit := True
      xorInput := False
    }
  }

  val Absorb = new Area {
    when(absorbBusy) {
      // Initialize Haraka componenent
      when(harakaInit) {
        haraka.io.init := True
        haraka.io.xnext := False
        haraka.io.xblock(mainBlock) := state

        harakaInit := False
        harakaStart := True
      }

      // Start Haraka process
      when(harakaStart) {
        haraka.io.xblock := 0
        haraka.io.init := False
        haraka.io.xnext := True
        harakaStart := False
        harakaBusy := True
      }

      // Wait for result
      when(harakaBusy && haraka.io.ready) {
        state := haraka.io.result
        harakaBusy := False

        // XOR Input but omit last round of absorbation
        when(!absorbCounter.willOverflowIfInc) {
          xorInput := True
        }

        when(absorbCounter.willOverflowIfInc) {
          remainderCtrl := True
        }
      }

      // Remainder logic. If input does not fit in the n bitrate blocks
      when(remainderCtrl) {
        state(xorRangeSelector) := state(xorRangeSelector) ^ Cat(remainder.reverse)
        absorbBusy := False
        remainderCtrl := False

        // Go to squeeze logic
        squeezeBusy := True
        harakaInit := True
      }

    }
  }

  val Squeeze = new Area {
    when(squeezeBusy) {
      // Initialize Haraka componenent
      when(harakaInit) {
        haraka.io.init := True
        haraka.io.xnext := False
        haraka.io.xblock(mainBlock) := state

        harakaInit := False
        harakaStart := True
      }

      // Start Haraka process
      when(harakaStart) {
        haraka.io.xblock := 0
        haraka.io.init := False
        haraka.io.xnext := True
        harakaStart := False
        harakaBusy := True
      }

      // Wait for result
      when(harakaBusy && haraka.io.ready) {
        state := haraka.io.result
        harakaBusy := False
        squeezeCounter.increment()

        // XOR Input but omit last round of absorbation
        when(!squeezeCounter.willOverflowIfInc) {
          harakaInit := True
        }

        when(squeezeCounter.willOverflowIfInc) {
          //remainderCtrl := True
          squeezeBusy := False
        }
      }
    }
  }

  // Result output
  // Caller can use the busy signal to wait for a new result, if init has been used to set a new input
  // value and next to start the calculation.
  io.ready := !busy
  io.result := state(c.blockTotalWidth - 1 downto c.blockTotalWidth - c.outputLen)
}
