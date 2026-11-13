package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.map.CoordGrid

internal object LibraryBooks : ObjReferences() {
    val reward = find("arceuus_library_reward")
    val texts = listOf(
        find("arceuus_library_censusrada"),
        find("arceuus_library_diaryricktor7"),
        find("arceuus_library_eathramrada"),
        find("arceuus_library_killingking"),
        find("arceuus_library_hosidiusletter"),
        find("arceuus_library_wintertodtparable"),
        find("arceuus_library_twillaccord"),
        find("arceuus_library_byrnespeech"),
        find("arceuus_library_ideologydarkness"),
        find("arceuus_library_radajourney"),
        find("arceuus_library_transvergencetheory"),
        find("arceuus_library_tristessatragedy"),
        find("arceuus_library_treacheryroyalty"),
        find("arceuus_library_teleportunlock"),
        find("arceuus_library_soulbearerbook"),
        find("hosidiusquest_book"),
    )
}

/** The five cyclic arrangements use the shelf index order recorded by RuneLite's Kourend Library plugin. */
internal class LibraryLayout(private val random:GameRandom) {
    private val shelves = requireNotNull(javaClass.getResourceAsStream("arceuus-shelves.tsv")).bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { row ->
            val (x,z,level) = row.split('\t').map(String::toInt)
            CoordGrid(x,z,level)
        }.toList()
    }
    private val sequences = listOf(
        listOf(3, 8, 9, 10, 11, 0, 12, 4, 1, 2, 15, 5, 6, 7, 14, 13),
        listOf(3, 8, 9, 10, 11, 0, 12, 4, 15, 1, 2, 14, 5, 6, 7, 13),
        listOf(1, 15, 2, 8, 0, 3, 12, 4, 7, 14, 5, 6, 9, 10, 11, 13),
        listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        listOf(0, 10, 12, 9, 3, 15, 7, 4, 11, 1, 8, 5, 6, 14, 2, 13),
    )
    private var expiresAt = 0
    private var byShelf = emptyMap<CoordGrid,List<Int>>()
    private var byText = emptyMap<Int,CoordGrid>()

    fun booksAt(coords:CoordGrid,minute:Int):List<Int> {
        refresh(minute)
        return byShelf[coords].orEmpty()
    }

    fun shelfFor(book:Int,minute:Int):CoordGrid {
        refresh(minute)
        return byText.getValue(book)
    }

    private fun refresh(minute:Int) {
        if (minute < expiresAt) return
        val sequence = random.pick(sequences)
        val start = random.of(shelves.size)
        val step = shelves.size / sequence.size
        byText = sequence.mapIndexed { index, book -> book to shelves[(start + index*step) % shelves.size] }.toMap()
        byShelf = byText.entries.groupBy({it.value},{it.key})
        expiresAt = minute + random.of(80..100)
    }
}
