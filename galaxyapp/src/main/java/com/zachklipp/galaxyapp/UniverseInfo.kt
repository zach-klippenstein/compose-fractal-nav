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
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Andromeda_Galaxy_560mm_FL.jpg/600px-Andromeda_Galaxy_560mm_FL.jpg",
        description = """
            The Andromeda Galaxy (IPA: /ænˈdrɒmɪdə/), also known as Messier 31, M31, or NGC 224 and
            originally the Andromeda Nebula (see below), is a barred spiral galaxy with diameter of
            about 220,000 ly approximately 2.5 million light-years (770 kiloparsecs) from Earth and
            the nearest large galaxy to the Milky Way. The galaxy's name stems from the area of
            Earth's sky in which it appears, the constellation of Andromeda, which itself is named
            after the Ethiopian (or Phoenician) princess who was the wife of Perseus in Greek
            mythology.
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Antennae",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Antennae_Galaxies_reloaded.jpg/600px-Antennae_Galaxies_reloaded.jpg",
        description = """
            The Antennae Galaxies (also known as NGC 4038/NGC 4039 or Caldwell 60/Caldwell 61) are a
            pair of interacting galaxies in the constellation Corvus. They are currently going
            through a starburst phase, in which the collision of clouds of gas and dust, with
            entangled magnetic fields, causes rapid star formation. They were discovered by William
            Herschel in 1785.
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Backward",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/NGC_4622HSTFull.jpg/600px-NGC_4622HSTFull.jpg",
        description = """
            NGC 4622 is a face-on unbarred spiral galaxy with a very prominent ring structure
            located in the constellation Centaurus. The galaxy is a member of the Centaurus Cluster.
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Black Eye",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/NGC_4826_-_HST.png/600px-NGC_4826_-_HST.png",
        description = """
            The Black Eye Galaxy (also called Sleeping Beauty Galaxy or Evil Eye Galaxy and
            designated Messier 64, M64, or NGC 4826) is a relatively isolated[7] spiral galaxy 17
            million light-years away in the mildly northern constellation of Coma Berenices. It was
            discovered by Edward Pigott in March 1779, and independently by Johann Elert Bode in
            April of the same year, as well as by Charles Messier the next year. A dark band of
            absorbing dust partially in front of its bright nucleus gave rise to its nicknames of
            the "Black Eye", "Evil Eye", or "Sleeping Beauty" galaxy.[10][11] M64 is well known
            among amateur astronomers due to its form in small telescopes and visibility across
            inhabited latitudes.
            (from Wikipedia)
        """.trimIndent(),
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
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/ESO-VLT-Laser-phot-33a-07.jpg/600px-ESO-VLT-Laser-phot-33a-07.jpg",
        description = """
            The Milky Way[a] is the galaxy that includes our Solar System, with the name describing
            the galaxy's appearance from Earth: a hazy band of light seen in the night sky formed
            from stars that cannot be individually distinguished by the naked eye. The term Milky
            Way is a translation of the Latin via lactea, from the Greek γαλακτικός κύκλος
            (galaktikos kýklos), meaning "milky circle."[20][21][22] From Earth, the Milky Way
            appears as a band because its disk-shaped structure is viewed from within. Galileo
            Galilei first resolved the band of light into individual stars with his telescope in
            1610. Until the early 1920s, most astronomers thought that the Milky Way contained all
            the stars in the Universe.[23] Following the 1920 Great Debate between the astronomers
            Harlow Shapley and Heber Curtis,[24] observations by Edwin Hubble showed that the Milky
            Way is just one of many galaxies.
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Needle",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Needle_Galaxy_4565.jpeg/600px-Needle_Galaxy_4565.jpeg",
        description = """
            NGC 4565 (also known as the Needle Galaxy or Caldwell 38) is an edge-on spiral galaxy
            about 30 to 50 million light-years away in the constellation Coma Berenices.[2] It lies
            close to the North Galactic Pole and has a visual magnitude of approximately 10. It is
            known as the Needle Galaxy for its narrow profile.[4] First recorded in 1785 by William
            Herschel, it is a prominent example of an edge-on spiral galaxy.[5]
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Wolf–Lundmark–Melotte",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/01/The_WLM_galaxy_on_the_edge_of_the_Local_Group.jpg/500px-The_WLM_galaxy_on_the_edge_of_the_Local_Group.jpg",
        description = """
            The Wolf–Lundmark–Melotte (WLM) is a barred irregular galaxy discovered in 1909 by Max
            Wolf, located on the outer edges of the Local Group. The discovery of the nature of the
            galaxy was accredited to Knut Lundmark and Philibert Jacques Melotte in 1926. It is in
            the constellation Cetus.
            (from Wikipedia)
        """.trimIndent(),
    ),
    Galaxy(
        name = "Pinwheel",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/M101_hires_STScI-PRC2006-10a.jpg/600px-M101_hires_STScI-PRC2006-10a.jpg",
    ),
    Galaxy(
        name = "Sculptor",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Sculptor_Galaxy_by_VISTA.jpg/560px-Sculptor_Galaxy_by_VISTA.jpg",
    ),
    Galaxy(
        name = "Sombrero",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5e/M104_ngc4594_sombrero_galaxy_hi-res.jpg/620px-M104_ngc4594_sombrero_galaxy_hi-res.jpg",
    ),
    Galaxy(
        name = "Southern Pinwheel",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Hubble_view_of_barred_spiral_galaxy_Messier_83.jpg/600px-Hubble_view_of_barred_spiral_galaxy_Messier_83.jpg",
    ),
    Galaxy(
        name = "Sunflower",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/M63_%28NGC_5055%29.jpg/600px-M63_%28NGC_5055%29.jpg",
    ),
    Galaxy(
        name = "Tadpole",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f9/UGC_10214HST.jpg/600px-UGC_10214HST.jpg",
    ),
    Galaxy(
        name = "Triangulum",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/VST_snaps_a_very_detailed_view_of_the_Triangulum_Galaxy.jpg/500px-VST_snaps_a_very_detailed_view_of_the_Triangulum_Galaxy.jpg",
    ),
    Galaxy(
        name = "Whirlpool",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/Messier51_sRGB.jpg/600px-Messier51_sRGB.jpg",
        description = """
            The Whirlpool Galaxy, also known as Messier 51a, M51a, and NGC 5194, is an interacting
            grand-design spiral galaxy with a Seyfert 2 active galactic nucleus.[5][6][7] It lies in
            the constellation Canes Venatici, and was the first galaxy to be classified as a spiral
            galaxy.[8] Its distance is 31 million light-years away from Earth.[9]
            The galaxy and its companion, NGC 5195,[10] are easily observed by amateur astronomers,
            and the two galaxies may be seen with binoculars.[11] The Whirlpool Galaxy has been
            extensively observed by professional astronomers, who study it to understand galaxy
            structure (particularly structure associated with the spiral arms) and galaxy
            interactions.
            (from Wikipedia)
        """.trimIndent(),
    ),
)

private val starsByGalaxy = mapOf(
    "Milky Way" to listOf(
        Star(
            name = "Sun",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b4/The_Sun_by_the_Atmospheric_Imaging_Assembly_of_NASA%27s_Solar_Dynamics_Observatory_-_20100819.jpg/440px-The_Sun_by_the_Atmospheric_Imaging_Assembly_of_NASA%27s_Solar_Dynamics_Observatory_-_20100819.jpg",
            description = """
                The Sun is the star at the center of the Solar System. It is a nearly perfect ball
                of hot plasma,[18][19] heated to incandescence by nuclear fusion reactions in its
                core, radiating the energy mainly as visible light, ultraviolet light, and infrared
                radiation. It is by far the most important source of energy for life on Earth. Its
                diameter is about 1.39 million kilometers (864,000 miles), or 109 times that of
                Earth. Its mass is about 330,000 times that of Earth, and it accounts for about
                99.86% of the total mass of the Solar System.[20] Roughly three quarters of the
                Sun's mass consists of hydrogen (~73%); the rest is mostly helium (~25%), with much
                smaller quantities of heavier elements, including oxygen, carbon, neon and iron.[21]
                (from Wikipedia)
            """.trimIndent(),
        )
    )
)

private val planetsByStar = mapOf(
    "Sun" to listOf(
        Planet(
            name = "Mercury",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Mercury_in_true_color.jpg/440px-Mercury_in_true_color.jpg",
            description = """
                Mercury is the smallest planet in the Solar System and the closest to the Sun. Its
                orbit around the Sun takes 87.97 Earth days, the shortest of all the Sun's planets.
                It is named after the Roman god Mercurius (Mercury), god of commerce, messenger of
                the gods, and mediator between gods and mortals, corresponding to the Greek god
                Hermes (Ἑρμῆς). Like Venus, Mercury orbits the Sun within Earth's orbit as an
                inferior planet, and its apparent distance from the Sun as viewed from Earth never
                exceeds 28°. This proximity to the Sun means the planet can only be seen near the
                western horizon after sunset or the eastern horizon before sunrise, usually in
                twilight. At this time, it may appear as a bright star-like object, but is more
                difficult to observe than Venus. From Earth, the planet telescopically displays the
                complete range of phases, similar to Venus and the Moon, which recurs over its
                synodic period of approximately 116 days.
                (from Wikipedia)
            """.trimIndent()
        ),
        Planet(
            name = "Venus",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Venus_from_Mariner_10.jpg/440px-Venus_from_Mariner_10.jpg",
            description = """
                Venus is the second planet from the Sun. It is named after the Roman goddess of love
                and beauty. As the brightest natural object in Earth's night sky after the Moon,
                Venus can cast shadows and can be visible to the naked eye in broad daylight.
                Venus's orbit is smaller than that of Earth, but its maximal elongation is 47°;
                thus, it can be seen not only near the Sun in the morning or evening, but also a
                couple of hours before or after sunrise or sunset, depending on the observer's
                latitude and on the positions of Venus and the Sun. Most of the time, it can be seen
                either in the morning or in the evening. At some times, it may even be seen a while
                in a completely dark sky. Venus orbits the Sun every 224.7 Earth days.[20] It has a
                synodic day length of 117 Earth days and a sidereal rotation period of 243 Earth
                days. Consequently, it takes longer to rotate about its axis than any other planet
                in the Solar System, and does so in the opposite direction to all but Uranus. This
                means that the Sun rises from its western horizon and sets in its east.[21] Venus
                does not have any moons, a distinction it shares only with Mercury among the planets
                in the Solar System.[22]
                (from Wikipedia)
            """.trimIndent()
        ),
        Planet(
            name = "Earth",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/The_Blue_Marble_%28remastered%29.jpg/440px-The_Blue_Marble_%28remastered%29.jpg",
            description = """
                Earth is the third planet from the Sun and the only astronomical object known to
                harbor life. While large amounts of water can be found throughout the Solar System,
                only Earth sustains liquid surface water. About 71% of Earth's surface is made up of
                the ocean, dwarfing Earth's polar ice, lakes, and rivers. The remaining 29% of
                Earth's surface is land, consisting of continents and islands. Earth's surface layer
                is formed of several slowly moving tectonic plates, interacting to produce mountain
                ranges, volcanoes, and earthquakes. Earth's liquid outer core generates the magnetic
                field that shapes Earth's magnetosphere, deflecting destructive solar winds.
                (from Wikipedia)
            """.trimIndent()
        ),
        Planet(
            name = "Mars",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/OSIRIS_Mars_true_color.jpg/440px-OSIRIS_Mars_true_color.jpg",
            description = """
                Mars is the fourth planet from the Sun and the second-smallest planet in the Solar
                System, being larger than only Mercury. In English, Mars carries the name of the
                Roman god of war and is often called the "Red Planet".[17][18] The latter refers to
                the effect of the iron oxide prevalent on Mars's surface, which gives it a striking
                reddish appearance in the sky.[19] Mars is a terrestrial planet with a thin
                atmosphere, with surface features such as impact craters, valleys, dunes, and polar
                ice caps.
                (from Wikipedia)
            """.trimIndent()
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
    val imageUrl: String,
    val description: String = "",
)

data class Star(
    val name: String,
    val imageUrl: String,
    val description: String = "",
)

data class Planet(
    val name: String,
    val imageUrl: String,
    val description: String = "",
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

