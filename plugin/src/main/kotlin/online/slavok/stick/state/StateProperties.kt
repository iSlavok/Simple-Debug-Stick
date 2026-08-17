package online.slavok.stick.state

import org.bukkit.Axis
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Lightable
import org.bukkit.block.data.MultipleFacing
import org.bukkit.block.data.Openable
import org.bukkit.block.data.Orientable
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.Rail
import org.bukkit.block.data.Snowable
import org.bukkit.block.data.Waterlogged
import org.bukkit.block.data.type.Bamboo
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.block.data.type.Wall

/**
 * One editable block-state property of a concrete [BlockData], with the same name Vanilla
 * uses (`facing`, `half`, `waterlogged`, ...) and its ordered value domain. Bukkit has no
 * generic "list a block's properties" API, so [of] derives them from the typed `BlockData`
 * interfaces a block implements. This covers the decorative properties the default config
 * targets (stairs, slabs, walls, fences, panes, bars, bamboo) and the common toggles;
 * blocks whose properties are not exposed as a typed interface are simply not editable
 * through the plugin.
 *
 * A [StateProperty] is bound to the `data` instance it was read from — cycle by cloning the
 * block's data, calling [set], then writing it back.
 */
class StateProperty(
    val name: String,
    val values: List<String>,
    private val getter: () -> String,
    private val setter: (String) -> Unit,
) {
    fun current(): String = getter()
    fun set(value: String) = setter(value)
}

object StateProperties {
    private val HORIZONTAL = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    private val BOOL = listOf("true", "false")

    /** Every editable property of [data], in a stable order. */
    fun of(data: BlockData): List<StateProperty> {
        val out = ArrayList<StateProperty>()

        (data as? Directional)?.let { d ->
            out += StateProperty(
                "facing",
                d.faces.map { it.name.lowercase() },
                { d.facing.name.lowercase() },
                { d.facing = BlockFace.valueOf(it.uppercase()) },
            )
        }
        (data as? Orientable)?.let { o ->
            out += StateProperty(
                "axis",
                o.axes.map { it.name.lowercase() },
                { o.axis.name.lowercase() },
                { o.axis = Axis.valueOf(it.uppercase()) },
            )
        }
        // Slab has its own type; other bisected blocks (stairs, doors, trapdoors) use half.
        (data as? Slab)?.let { s ->
            out += StateProperty(
                "type",
                Slab.Type.entries.map { it.name.lowercase() },
                { s.type.name.lowercase() },
                { s.type = Slab.Type.valueOf(it.uppercase()) },
            )
        } ?: (data as? Bisected)?.let { b ->
            out += StateProperty(
                "half",
                Bisected.Half.entries.map { it.name.lowercase() },
                { b.half.name.lowercase() },
                { b.half = Bisected.Half.valueOf(it.uppercase()) },
            )
        }
        (data as? Stairs)?.let { s ->
            out += StateProperty(
                "shape",
                Stairs.Shape.entries.map { it.name.lowercase() },
                { s.shape.name.lowercase() },
                { s.shape = Stairs.Shape.valueOf(it.uppercase()) },
            )
        }
        (data as? Rail)?.let { r ->
            out += StateProperty(
                "shape",
                r.shapes.map { it.name.lowercase() },
                { r.shape.name.lowercase() },
                { r.shape = Rail.Shape.valueOf(it.uppercase()) },
            )
        }
        (data as? Ageable)?.let { a ->
            out += StateProperty(
                "age",
                (0..a.maximumAge).map { it.toString() },
                { a.age.toString() },
                { a.age = it.toInt() },
            )
        }
        (data as? Levelled)?.let { l ->
            out += StateProperty(
                "level",
                (0..l.maximumLevel).map { it.toString() },
                { l.level.toString() },
                { l.level = it.toInt() },
            )
        }
        (data as? Bamboo)?.let { b ->
            out += StateProperty(
                "leaves",
                Bamboo.Leaves.entries.map { it.name.lowercase() },
                { b.leaves.name.lowercase() },
                { b.leaves = Bamboo.Leaves.valueOf(it.uppercase()) },
            )
        }
        (data as? Powerable)?.let { p -> out += toggle("powered", { p.isPowered }, { p.isPowered = it }) }
        (data as? Openable)?.let { o -> out += toggle("open", { o.isOpen }, { o.isOpen = it }) }
        (data as? Lightable)?.let { l -> out += toggle("lit", { l.isLit }, { l.isLit = it }) }
        (data as? Snowable)?.let { s -> out += toggle("snowy", { s.isSnowy }, { s.isSnowy = it }) }
        (data as? Waterlogged)?.let { w -> out += toggle("waterlogged", { w.isWaterlogged }, { w.isWaterlogged = it }) }

        // Walls: an `up` post plus per-side heights. Checked before MultipleFacing because
        // a wall's connections are heights (none/low/tall), not booleans.
        (data as? Wall)?.let { wall ->
            out += toggle("up", { wall.isUp }, { wall.isUp = it })
            for (face in HORIZONTAL) {
                out += StateProperty(
                    face.name.lowercase(),
                    Wall.Height.entries.map { it.name.lowercase() },
                    { wall.getHeight(face).name.lowercase() },
                    { wall.setHeight(face, Wall.Height.valueOf(it.uppercase())) },
                )
            }
        } ?: (data as? MultipleFacing)?.let { mf ->
            for (face in mf.allowedFaces) {
                out += toggle(
                    face.name.lowercase(),
                    { mf.hasFace(face) },
                    { mf.setFace(face, it) },
                )
            }
        }

        return out
    }

    private inline fun toggle(
        name: String,
        crossinline getter: () -> Boolean,
        crossinline setter: (Boolean) -> Unit,
    ): StateProperty = StateProperty(name, BOOL, { getter().toString() }, { setter(it.toBoolean()) })
}
