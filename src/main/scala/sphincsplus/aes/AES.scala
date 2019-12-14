package sphincsplus.aes

import spinal.core._

class AES extends Component {
  val io = new Bundle {
    val encdec = in Bool
    val init = in Bool
    val xnext = in Bool
    val ready = out Bool

    val key = in Bits(256 bits)
    val keylen = in Bool // => 0=128 Bit, 1=256 Bit

    val xblock = in Bits(128 bits)
    val result = out Bits(128 bits)
    val result_valid = out Bool
  }

  val aes_inst = new aes_core()
  io.encdec <> aes_inst.io.encdec
  io.init <> aes_inst.io.init
  io.xnext <> aes_inst.io.xnext
  io.ready <> aes_inst.io.ready
  io.key <> aes_inst.io.key
  io.keylen <> aes_inst.io.keylen
  io.xblock <> aes_inst.io.xblock
  io.result <> aes_inst.io.result
  io.result_valid <> aes_inst.io.result_valid

}