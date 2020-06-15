/*
 * The MIT License (MIT)
 * Copyright (c) 2020, Jens Nazarenus
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
import sphincsplus.Params.HARAKA_512
import spinal.core._
import spinal.lib.Counter
import sphincsplus.utils.SphincsPlusUtils.BitVectorExtension

// Construction parameters for a Fors instance
case class ForsConfig(n : Int = 16,
                      forsTrees : Int = 10,
                      forsHeight : Int = 15) {
  val forsSigBytes = ((forsHeight + 1) * forsTrees * n)
  val forsMsgBytes = ((forsHeight * forsTrees + 7) / 8)
  val forsPkBytes = n

  // TODO Print invalid parameter warnings/errors here with 'SpinalWarning()' or assert:
  // assert(
  //   assertion = n == 0,
  //   message = "Invalid value for n",
  //   severity = ERROR
  // )
}


// Input/Output of a Fors instance
class ForsIo(g: ForsConfig) extends Bundle {
  val addr = in Bits(256 bits)
  val message = in Bits(8 * g.forsMsgBytes bits)

  val result_sig = out Vec(Bits(128 bits), g.forsTrees * g.n)
  val result_pk = out Vec(Bits(8 bits), g.forsPkBytes)
  val init = in Bool
  val xnext = in Bool
  val ready = out Bool
}

class Fors(g: ForsConfig) extends Component {

  /**
   * Calculates the j-th message index
   * @param m the message at a specific position
   * @param j the index
   * @return the message index
   */
  def to_message_idx(m: UInt, j: UInt, offset: UInt) : UInt = {
    (((m >> (offset & 0x7)) & 0x1) << j.resize(8))
  }

  val io = new ForsIo(g)

  // fire values
  val prepareAddresses = Reg(Bool) init(False)
  val prepTreeAddress = Reg(Bool) init(False)
  val calcSkHaraka = Reg(Bool) init(False)
  val calcIndices = Reg(Bool) init(False)
  val initCalcSk = Reg(Bool) init(False)
  val treeHashGenSk = Reg(Bool) init(False)
  val treeHashGenLeaf = Reg(Bool) init(False)

  // Haraka
  val haraka = new Haraka(new HarakaConfig(256))
  haraka.io.xblock := 0
  haraka.io.init := False
  haraka.io.xnext := False
  val harakaInit = Reg(Bool) init(False)
  val harakaBusy = Reg(Bool) init(False)
  val harakaStart = Reg(Bool) init(False)

  def harakaCfg = new HarakaConfig(HARAKA_512)
  val harakaS = new HarakaSpongeConstr(new HarakaSpongeConstrConfig(512, 256, 256, harakaCfg))
  harakaS.io.xblock := 0
  harakaS.io.init := False
  harakaS.io.xnext := False
  val harakaSpongeInit = Reg(Bool) init(False)
  val harakaSpongeBusy = Reg(Bool) init(False)
  val harakaSpongeStart = Reg(Bool) init(False)

  // Counter
  val forsTreeCounter = Counter(g.forsTrees)

  val busy = Reg(Bool) init(False)
  val message = Reg(Vec(Bits(8 bits), g.forsMsgBytes))
  val indices = Reg(Vec(Bits(32 bits), g.forsTrees))
  val sig = Reg(Vec(Bits(128 bits), g.forsTrees * g.n)) keep()

  // Treehash
  val leafCount = (1 << g.forsHeight)
  val leafCounter = Counter(leafCount)
  //val leafs = Vec(Vec((Bits())))


  val AddressCtrl = new Area {
    val in = RegNextWhen(Address(), io.init)
    val forsTreeAddr = Reg(Address())

    when(prepareAddresses) {
      // TODO what to do with the address
      forsTreeAddr.setAddrType(Params.ADDR_TYPE_FORSTREE)
      prepareAddresses := False
    }
  }

  val Indices = new Area {
    val curTree = Reg(UInt(log2Up(g.forsTrees) bits)) init(0) keep()
    val offset = (curTree * g.forsHeight) keep()
    val seqList = List.range(0, g.forsHeight)

    when(calcIndices && curTree < g.forsTrees) {
      val idxList = seqList.map(x => to_message_idx(message(offset + x >> 3).asUInt, x, (offset + x)))
      indices(curTree) := (idxList.foldLeft(U(0))(_^_)).asBits.resize(32)
      curTree := curTree + 1
    }

    when(calcIndices && curTree === g.forsTrees) {
      calcIndices := False
      initCalcSk := True
      prepTreeAddress := True
    }
  }

  val Init = new Area {
    when(io.init && !busy) {
      message := io.message.subdivideInRev(8 bits)
      AddressCtrl.in.assignFromBits(io.addr)
      AddressCtrl.forsTreeAddr.assignFromBits(io.addr)
      prepareAddresses := True
      indices.map(_ := B"0000_0000".resize(32))
      forsTreeCounter.clear()
    }

    when(io.xnext && !busy) {
      calcIndices := True
      busy := True
    }
  }

  val GenSk = new Area {

    // Prepare the GenSk calculation. Prepare address dependent on the current
    // forsTreeCounter value
    when(initCalcSk && prepTreeAddress) {
      val treeIndex = indices(forsTreeCounter).asUInt + (forsTreeCounter * (1 << g.forsHeight))
      AddressCtrl.forsTreeAddr.setTreeHeight(U(0))
      AddressCtrl.forsTreeAddr.setTreeIndex(treeIndex)
      prepTreeAddress := False
      harakaInit := True
    }

    when(initCalcSk && harakaInit) {
      haraka.io.init := True
      haraka.io.xnext := False
      haraka.io.xblock := AddressCtrl.forsTreeAddr.asBits

      harakaInit := False
      harakaStart := True
    }

    // Start Haraka process
    when(initCalcSk && harakaStart) {
      haraka.io.xblock := 0
      haraka.io.init := False
      haraka.io.xnext := True
      harakaStart := False
      harakaBusy := True
    }

    // Wait for result
    when(initCalcSk && harakaBusy && haraka.io.ready) {
      harakaBusy := False
      //sig(0).assignFromBits(haraka.io.result(255 downto 128))
      sig(0) := haraka.io.result(255 downto 128)
      initCalcSk := False
      treeHashGenSk := True
      prepTreeAddress := True
      harakaInit := True
    }
  }

  val Treehash = new Area {
    val offset = Reg(UInt(8 bits)) init(0)
    val stack = Reg(Vec(Bits(8 bits), g.forsHeight * g.n))

    when(treeHashGenSk && prepTreeAddress) {
      val treeIndex = indices(forsTreeCounter).asUInt + (forsTreeCounter * (1 << g.forsHeight))
      AddressCtrl.forsTreeAddr.setTreeHeight(U(0))
      AddressCtrl.forsTreeAddr.setTreeIndex(offset + leafCounter.value)
      prepTreeAddress := False
      harakaInit := True
    }
    when(treeHashGenSk && harakaInit) {
      haraka.io.init := True
      haraka.io.xnext := False
      haraka.io.xblock := AddressCtrl.forsTreeAddr.asBits

      harakaInit := False
      harakaStart := True
    }
    when(treeHashGenSk && harakaStart) {
      haraka.io.xblock := 0
      haraka.io.init := False
      haraka.io.xnext := True
      harakaStart := False
      harakaBusy := True
    }

    // Wait for result
    when(treeHashGenSk && harakaBusy && haraka.io.ready) {
      harakaBusy := False
      //sig(0).assignFromBits(haraka.io.result(255 downto 128))
      sig(1) := haraka.io.result(255 downto 128)
      prepTreeAddress := True
      treeHashGenSk := True
      harakaInit := True
    }
  }

  // DEBUG VALUES
  // -------------
  val dbg_forsTreeAddr = AddressCtrl.forsTreeAddr.asBits keep()
  // -------------

  for(j <- 0 until 160) {
    io.result_sig(j) := 0
  }
  io.result_pk.map(_ := B"0000_0000")
  io.ready := leafCounter.willOverflow
}