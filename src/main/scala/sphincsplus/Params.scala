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

object Params {

  // Tree height in bytes
  def FORS_N = 16

  def FORS_MSG_BYTES = ((FORS_HEIGHT * FORS_TREES + 7) / 8)
  def FORS_SIG_BYTES = ((FORS_HEIGHT + 1) * FORS_TREES * FORS_N)
  def FORS_PK_BYTES = FORS_N

  // Tree dimensions
  def FORS_TREES = 10
  def FORS_HEIGHT = 15

  // Address types
  def ADDR_TYPE_WOTS = B"0000".resize(32)      // 0
  def ADDR_TYPE_WOTSPK = B"0001".resize(32)    // 1
  def ADDR_TYPE_HASHTREE = B"0010".resize(32)  // 2
  def ADDR_TYPE_FORSTREE = B"0011".resize(32)  // 3
  def ADDR_TYPE_FORSPK = B"00100".resize(32)   // 4

}