package com.musicflow.app.ui.galaxy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.musicflow.app.ui.theme.MFColors

/**
 * Represents a single interactive node within the Music Galaxy view.
 *
 * Nodes model different music library entities — artists, albums,
 * playlists, and favorited tracks — positioned in a 2D interactive space.
 *
 * @property id Unique identifier (songId for tracks, playlist name for playlists, etc.).
 * @property label Display label shown beneath the node.
 * @property type The category of music entity this node represents.
 * @property position Current (x, y) position in the galaxy canvas coordinate space.
 * @property size Diameter of the node circle in dp.
 * @property color Accent tint for the node ring and glow.
 * @property artworkUrl URL to load the node's artwork image.
 * @property weight Relative importance (0f..1f) — scales glow intensity.
 */
data class GalaxyNode(
    val id: String,
    val label: String,
    val type: GalaxyNodeType,
    val position: Offset,
    val size: Float = 64f,
    val color: Color = MFColors.Accent,
    val artworkUrl: String = "",
    val weight: Float = 0.5f,
)

/**
 * Categorises the entity backing a [GalaxyNode].
 *
 * Each type maps to a distinct visual ring style and default colour.
 */
enum class GalaxyNodeType(val label: String) {
    ARTIST("Artist"),
    ALBUM("Album"),
    PLAYLIST("Playlist"),
    FAVORITE("Favorite"),
}

/**
 * Default accent colour for each [GalaxyNodeType].
 */
fun GalaxyNodeType.defaultColor(): Color = when (this) {
    GalaxyNodeType.ARTIST -> MFColors.Accent
    GalaxyNodeType.ALBUM -> MFColors.Secondary
    GalaxyNodeType.PLAYLIST -> MFColors.Tertiary
    GalaxyNodeType.FAVORITE -> MFColors.Error
}

/**
 * A relationship edge between two [GalaxyNode]s rendered as a connecting line.
 *
 * @property fromId Source node id.
 * @property toId Target node id.
 * @property strength Edge weight 0f..1f controlling line opacity.
 */
data class GalaxyEdge(
    val fromId: String,
    val toId: String,
    val strength: Float = 0.5f,
)
