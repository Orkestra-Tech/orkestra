package io.chumps.orchestra.utils

object Colours {

  def generate(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
