package org.rsmod.content.skills.fletching.bowstringing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BowStringingScript
@Inject
constructor(
    private val stringing: BowStringing,
    private val recipes: BowStringingRecipeRegistry,
    private val spool: BowStringSpool,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(objs.bow_string, recipe.unstrung) { stringing.select(this, recipe) }
            onOpHeldU(objs.bow_string_spool, recipe.unstrung) {
                stringing.select(this, recipe, BowStringSource.Spool)
            }
        }
        onOpHeld1(objs.bow_string_spool) { spool.fill(this) }
        onOpHeld2(objs.bow_string_spool) { spool.check(this) }
        onOpHeldU(objs.bow_string, objs.bow_string_spool) { spool.deposit(this, objs.bow_string) }
        onOpHeldU(objs.noted_bow_string, objs.bow_string_spool) {
            spool.deposit(this, objs.noted_bow_string)
        }
        onOpHeldU(objs.bow_string_spool, objs.bow_string_spool) { spool.combine(this) }
        onPlayerQueueWithArgs<BowStringingRequest>(fletching_queues.bowStringing) {
            stringing.process(this, it.args)
        }
    }
}
