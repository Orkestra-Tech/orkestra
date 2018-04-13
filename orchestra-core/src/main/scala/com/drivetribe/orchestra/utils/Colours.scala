package com.drivetribe.orchestra.utils

object Colours {

  /**
    * Generate a 6 hexa colour based on the given String.
    */
  def generate(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
