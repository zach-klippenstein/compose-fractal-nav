package com.zachklipp.galaxyapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val knownGalaxies = listOf(
    Galaxy(
        name = "Andromeda",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Andromeda_Galaxy_560mm_FL.jpg/600px-Andromeda_Galaxy_560mm_FL.jpg"
    ),
    Galaxy(
        name = "Antennae",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Antennae_Galaxies_reloaded.jpg/600px-Antennae_Galaxies_reloaded.jpg",
    ),
    Galaxy(
        name = "Backward",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/NGC_4622HSTFull.jpg/600px-NGC_4622HSTFull.jpg"
    ),
    Galaxy(
        name = "Black Eye",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/NGC_4826_-_HST.png/600px-NGC_4826_-_HST.png"
    ),
    Galaxy(
        name = "Bode's",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Messier_81_HST.jpg/600px-Messier_81_HST.jpg"
    ),
    Galaxy(
        name = "Butterfly",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/NGC_4567_%26_4568.png/600px-NGC_4567_%26_4568.png"
    ),
    Galaxy(
        name = "Cartwheel",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/26/Cartwheel_Galaxy.jpg/500px-Cartwheel_Galaxy.jpg"
    ),
    Galaxy(
        name = "Cigar",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ce/M82_HST_ACS_2006-14-a-large_web.jpg/600px-M82_HST_ACS_2006-14-a-large_web.jpg"
    ),
    Galaxy(
        name = "Circinus",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/142_circinus_galaxy.png/600px-142_circinus_galaxy.png"
    ),
    Galaxy(
        name = "Milky Way",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/ESO-VLT-Laser-phot-33a-07.jpg/600px-ESO-VLT-Laser-phot-33a-07.jpg"
    ),
)

private val starsByGalaxy = mapOf(
    "Milky Way" to listOf(
        Star(
            name = "Sun",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/The_Sun_by_the_Atmospheric_Imaging_Assembly_of_NASA%27s_Solar_Dynamics_Observatory_-_20100819.jpg/440px-The_Sun_by_the_Atmospheric_Imaging_Assembly_of_NASA%27s_Solar_Dynamics_Observatory_-_20100819.jpg"
        )
    )
)

private val planetsByStar = mapOf(
    "Sun" to listOf(
        Planet(
            name = "Mercury",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Mercury_in_true_color.jpg/440px-Mercury_in_true_color.jpg"
        ),
        Planet(
            name = "Venus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Venus_from_Mariner_10.jpg/440px-Venus_from_Mariner_10.jpg"
        ),
        Planet(
            name = "Earth",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/The_Blue_Marble_%28remastered%29.jpg/440px-The_Blue_Marble_%28remastered%29.jpg"
        ),
        Planet(
            name = "Mars",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/OSIRIS_Mars_true_color.jpg/440px-OSIRIS_Mars_true_color.jpg"
        ),
    )
)

class UniverseInfo {
    val galaxies: StateFlow<List<Galaxy>?> = MutableStateFlow(knownGalaxies)

    fun getStars(galaxy: Galaxy): StateFlow<List<Star>?> {
        return MutableStateFlow(starsByGalaxy[galaxy.name])
    }

    fun getPlanets(star: Star): StateFlow<List<Planet>?> {
        return MutableStateFlow(planetsByStar[star.name])
    }
}

data class Galaxy(
    val name: String,
    val imageUrl: String
)

data class Star(
    val name: String,
    val imageUrl: String,
)

data class Planet(
    val name: String,
    val imageUrl: String,
)

@Stable
fun PolarOffset(angle: Float, radius: Float): PolarOffset = PolarOffset(Offset(angle, radius))

@JvmInline
@Immutable
value class PolarOffset internal constructor(private val offset: Offset) {
    @Stable
    val angle: Float
        get() = offset.x

    @Stable
    val radius: Float
        get() = offset.y

    @Stable
    val isSpecified: Boolean
        get() = offset.isSpecified

    @Stable
    override fun toString(): String {
        return if (isSpecified) {
            "PolarOffset(angle=$angle, radius=$radius)"
        } else {
            "PolarOffset.Unspecified"
        }
    }

    companion object {
        @Stable
        val Zero = PolarOffset(Offset.Zero)

        @Stable
        val Unspecified = PolarOffset(Offset.Unspecified)
    }
}

