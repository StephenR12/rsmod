package org.rsmod.content.travel.mausoleumbridge

import org.rsmod.api.type.refs.loc.LocReferences

public object MausoleumBridgeLocs : LocReferences() {
    public val surface = find("fenk_bridgesurface", 2171156116996093930)
    public val mainlandUnrepaired = find("fenk_bridgebroken", 2171156116996093931)
    public val islandUnrepaired = find("fenk_bridgebroken_mirror", 2171156116996146481)
    public val mainlandBuild = find("fenk_bridge_build", 1700804965951581112)
    public val islandBuild = find("fenk_bridge_build_mirror", 1700804965951581113)
    public val mainlandRepaired = find("fenk_bridge_repaired", 2171156116996146484)
    public val islandRepaired = find("fenk_bridge_repaired_mirror", 2171156116996146485)
}
