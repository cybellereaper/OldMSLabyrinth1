package com.github.cybellereaper.house

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PlayerHouseManager(private val instanceManager: InstanceManager) {
    private val playerHouses = ConcurrentHashMap<String, Instance>()
    private val housePermissions = ConcurrentHashMap<String, MutableMap<UUID, HousePermission>>()
    private val houseRegions = ConcurrentHashMap<String, MutableMap<String, HouseRegion>>()

    fun addPlayerPermission(owner: String, player: Player, permission: HousePermission) {
        housePermissions.computeIfAbsent(owner) { ConcurrentHashMap() }[player.uuid] = permission
    }

    fun createRegion(owner: String, regionName: String, minPos: Pos, maxPos: Pos, permission: HousePermission) {
        val region = HouseRegion(minPos, maxPos, permission)
        houseRegions.computeIfAbsent(owner) { ConcurrentHashMap() }[regionName] = region
    }

    fun registerEvents(eventNode: EventNode<Event>) {
        eventNode.addListener(PlayerBlockBreakEvent::class.java) { event ->
            val player = event.player
            val instance = event.instance
            val pos = event.blockPosition

            val owner = playerHouses.entries.find { it.value == instance }?.key
            if (owner != null) {
                if (!hasPermissionAtLocation(owner, player, Pos(pos.x, pos.y, pos.z), PermissionType.BREAK)) {
                    event.isCancelled = true
                }
            }
        }

        eventNode.addListener(PlayerBlockPlaceEvent::class.java) { event ->
            val player = event.player
            val instance = event.instance
            val pos = event.blockPosition

            val owner = playerHouses.entries.find { it.value == instance }?.key
            if (owner != null) {
                if (!hasPermissionAtLocation(owner, player, Pos(pos.x, pos.y, pos.z), PermissionType.BUILD)) {
                    event.isCancelled = true
                }
            }
        }

        eventNode.addListener(PlayerMoveEvent::class.java) { event ->
            val player = event.player
            val instance = event.instance
            val pos = event.newPosition

            val owner = playerHouses.entries.find { it.value == instance }?.key
            if (owner != null) {
                if (!hasPermissionAtLocation(owner, player, pos, PermissionType.ENTER)) {
                    event.newPosition = event.player.position
                }
            }
        }
    }

    private fun hasPermissionAtLocation(owner: String, player: Player, pos: Pos, type: PermissionType): Boolean {
        // Owner has all permissions
        if (player.username == owner) return true

        // Check global permissions first
        val globalPermission = housePermissions[owner]?.get(player.uuid)
        if (globalPermission != null) {
            when (type) {
                PermissionType.BUILD -> if (globalPermission.canBuild) return true
                PermissionType.BREAK -> if (globalPermission.canBreak) return true
                PermissionType.ENTER -> if (globalPermission.canEnter) return true
            }
        }

        // Check region-specific permissions
        houseRegions[owner]?.values?.forEach { region ->
            if (isPositionInRegion(pos, region)) {
                when (type) {
                    PermissionType.BUILD -> if (region.permissions.canBuild) return true
                    PermissionType.BREAK -> if (region.permissions.canBreak) return true
                    PermissionType.ENTER -> if (region.permissions.canEnter) return true
                }
            }
        }

        return false
    }

    private fun isPositionInRegion(pos: Pos, region: HouseRegion): Boolean {
        return pos.x >= region.minPos.x && pos.x <= region.maxPos.x &&
                pos.y >= region.minPos.y && pos.y <= region.maxPos.y &&
                pos.z >= region.minPos.z && pos.z <= region.maxPos.z
    }

    enum class PermissionType {
        BUILD, BREAK, ENTER
    }
}