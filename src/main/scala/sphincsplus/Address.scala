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
import spinal.core._

trait AddressFactory {
  def Address() = new Address()
}

case class Address() extends Bundle {
  val funct1 = Bits(32 bits) // hash address or tree index
  val funct0 = Bits(32 bits) // chain address or tree height
  val keypair_addr = Bits(32 bits)
  val addr_type = Bits(32 bits)
  val tree_addr = Bits(96 bits)
  val layer_addr = Bits(32 bits)

  def setTreeHeight(x: UInt): Unit = {
    funct0 := x.asBits.resize(32)
  }

  def setTreeIndex(x: UInt): Unit = {
    funct1 := x.asBits.resize(32)
  }

  def setAddrType(x: UInt): Unit = {
    addr_type := x.asBits.resize(32)
  }

}
