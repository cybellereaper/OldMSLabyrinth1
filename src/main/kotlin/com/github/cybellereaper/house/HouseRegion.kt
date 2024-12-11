package com.github.cybellereaper.house

import net.minestom.server.coordinate.Pos

class HouseRegion(
    val minPos: Pos,
    val maxPos: Pos,
    val permissions: HousePermission = HousePermission()
)