package org.rsmod.content.skills.firemaking

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.queue.QueueReferences

public typealias firemaking_locs = FiremakingLocs

public object FiremakingLocs : LocReferences() {
    public val fire = find("fire")
    public val redFire = find("red_fire")
    public val greenFire = find("green_fire")
    public val blueFire = find("blue_fire")
    public val purpleFire = find("trail_purple_fire")
    public val whiteFire = find("trail_white_fire")
    public val funeralPyre = find("temple_pyre")
    public val stoneStand = find("pyre_stand")
    public val funeralPyreLogs = find("temple_pyre_logs")
    public val funeralPyreOak = find("temple_pyre_oak")
    public val funeralPyreWillow = find("temple_pyre_willow")
    public val funeralPyreMaple = find("temple_pyre_maple")
    public val funeralPyreYew = find("temple_pyre_yew")
    public val funeralPyreMagic = find("temple_pyre_magic")
    public val funeralPyreTeak = find("temple_pyre_teak")
    public val funeralPyreMahogany = find("temple_pyre_mahogany")
    public val funeralPyreArcticPine = find("temple_pyre_arctic_pine")
    public val funeralPyreRedwood = find("temple_pyre_redwood")
    public val funeralPyreRemainsLogs = find("temple_pyre_bones_logs")
    public val funeralPyreRemainsOak = find("temple_pyre_bones_oak")
    public val funeralPyreRemainsWillow = find("temple_pyre_bones_willow")
    public val funeralPyreRemainsMaple = find("temple_pyre_bones_maple")
    public val funeralPyreRemainsYew = find("temple_pyre_bones_yew")
    public val funeralPyreRemainsMagic = find("temple_pyre_bones_magic")
    public val funeralPyreRemainsTeak = find("temple_pyre_bones_teak")
    public val funeralPyreRemainsMahogany = find("temple_pyre_bones_mahogany")
    public val funeralPyreRemainsArcticPine = find("temple_pyre_bones_arctic_pine")
    public val funeralPyreRemainsRedwood = find("temple_pyre_bones_redwood")
    public val barbarianPyreSite = find("brut_burned_ground")
    public val barbarianPyreStage1 = find("brut_pyre_boat_station_stage1")
    public val barbarianPyreStage2 = find("brut_pyre_boat_station_stage2")
    public val barbarianPyreStage3 = find("brut_pyre_boat_station_stage3")
    public val barbarianPyreBones = find("brut_pyre_boat_station_stage3_bones")
    public val barbarianPyreFire = find("brut_pyre_boat_station_on_fire")
    public val barbarianPyreLaunched = find("brut_pyre_boat_station_launched")
    public val barbarianPyreReady = find("brut_pyre_boat_station_ready")
    public val barbarianPyreSink = find("brut_pyre_boat_station_sink")
    public val lampOilStillEmpty = find("lamp_oil_still_0")
    public val lampOilStillFull = find("lamp_oil_still_1")
    public val dorgeshLampStill = find("dorgesh_market_still")
}

public typealias firemaking_queues = FiremakingQueues

public object FiremakingQueues : QueueReferences() {
    public val attempt = find("firemaking_attempt")
    public val pyreLogPreparation = find("firemaking_pyre_log_preparation")
}
