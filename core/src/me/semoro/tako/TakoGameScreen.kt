package me.semoro.tako

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random


val level = """
    ##################
    #                #
    # ######### #  # #
    #           #  # #
    # ###### ##### # #
    # #M     # M   # #
    # # #### #  #### #
    # # #  #         #
    # #       # # ####
    # #### #### #    #
    #T          # #  #
    ########### # ####
    #M               #
    ##################
""".trimIndent()



data class WallBlock(
    val x: Float, val y: Float,
    val size: Float
)


data class Animated(
    var x: Float, var y: Float,
    var tx: Float, var ty: Float,
    var tState: Float
)

data class Mumeii(
    val animated: Animated,
    var dir: MotionDirection
)

data class Cookie(
    var x: Float, var y: Float
)

data class Tako(
    val animated: Animated,
    var dir: MotionDirection
)

data class Cell(
    var wall: WallBlock? = null
)

class MapU(private val width: Int, height: Int) {
    private val internal = Array(width * height) { Cell() }

    operator fun get(x: Float, y: Float): Cell? {
        return internal.getOrNull(
            x.toInt() + y.toInt() * width
        )
    }
}

enum class MotionDirection(val x: Float, val y: Float) {
    UP(0f, 1f),
    RIGHT(1f, 0f),
    DOWN(0f, -1f),
    LEFT(-1f, 0f)
}

class EndingPlayer(
    val texture: Texture,
    val sound: Sound,
    var timeLeft: Float
)

class TakoGameScreen(
    val game: TakoGame,
    val difficulty: Difficulty
) : ScreenAdapter() {
    private var batch: SpriteBatch? = null
    private var background: Texture? = null
    private var walls: Texture? = null
    private var mumeii: Texture? = null
    private var tako: Texture? = null
    private var cookie: Texture? = null
    private var win: Texture? = null
    private var lose: Texture? = null
    private var winSound: Sound? = null
    private var loseSound: Sound? = null
    private var music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3")).apply {
        isLooping = true
        volume = 0.1f
    }
    private val hardStartSound = Gdx.audio.newMusic(Gdx.files.internal("ina-its-time-for-pain.mp3"))

    init {
        println(difficulty)
        if (difficulty == Difficulty.Takodachi) {
            hardStartSound.volume = 0.3f
            hardStartSound.setOnCompletionListener {
                music.play()
            }
            hardStartSound.play()

        } else {
            music.play()
        }
    }

    lateinit var cam: OrthographicCamera


    private fun checkCollision(x: Float, y: Float): Boolean {
        return mapU!![x, y]?.wall != null
    }

    private val blocks = mutableListOf<WallBlock>()
    private val mumeiis = mutableListOf<Mumeii>()
    private val cookies = mutableListOf<Cookie>()
    private val takodachi = Tako(Animated(0f, 0f, 0f, 0f, 0f), MotionDirection.UP)



    fun create() {

        win = Texture("win.png")
        lose = Texture("lose.jpeg")

        background = Texture("background.png").apply { setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }
        walls = Texture("walls.png").apply { setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat) }

        mumeii = Texture("mumeii.png")
        tako = Texture("tako.png")

        cookie = Texture("cookie.png")

        winSound = Gdx.audio.newSound(Gdx.files.internal("ina-wah-echo.mp3"))
        loseSound = Gdx.audio.newSound(Gdx.files.internal("lose.mp3"))


        batch = SpriteBatch()

        cam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        resetMap()
    }

    init {
        create()
    }


    var mapU: MapU? = null

    fun resetMap() {

        blocks.clear()
        cookies.clear()
        mumeiis.clear()

        var maxX = 0
        var maxY = 0

        for ((y, line) in level.trim().lines().withIndex()) {
            for ((x, c) in line.trim().withIndex()) {
                if (c == '#') {
                    blocks.add(
                        WallBlock(
                            x.toFloat(),
                            y.toFloat(),
                            size = 50f
                        )
                    )
                }
                if (c == 'M') {
                    mumeiis.add(
                        Mumeii(
                            Animated(x.toFloat(), y.toFloat(),
                                x.toFloat(), y.toFloat(),
                                0f),
                            MotionDirection.UP
                        )
                    )
                }
                if (c == 'T') {
                    takodachi.animated.apply {
                        this.x = x.toFloat()
                        this.y = y.toFloat()
                        this.tx = this.x
                        this.ty = this.y
                    }
                }
                if (c == ' ' || c == 'M') {
                    cookies += Cookie(
                        x.toFloat(), y.toFloat()
                    )
                }
                maxX = max(maxX, x)
            }
            maxY = max(maxY, y)
        }

        mapU = MapU(maxX + 1, maxY + 1)
        blocks.forEach {
            mapU!![it.x, it.y]?.wall = it
        }
    }




    fun renderDBox(texture: Texture, x: Float, y: Float, width: Float, height: Float, rot: Float) {
        val xy0 = Vector2(x, y).rotateDeg(-rot)
        val xy1 = Vector2(x, y + height).rotateDeg(-rot)
        val xy2 = Vector2(x + width, y + height).rotateDeg(-rot)
        val xy3 = Vector2(x + width, y).rotateDeg(-rot)

        fun makeUV(vector2: Vector2): Vector2 {
            val u = (Gdx.graphics.width / 2f + vector2.x) / Gdx.graphics.width
            val v = (Gdx.graphics.height / 2f + vector2.y) / Gdx.graphics.height
            return Vector2(u, v)
        }

        val uv0 = makeUV(xy0)
        val uv1 = makeUV(xy1)
        val uv2 = makeUV(xy2)
        val uv3 = makeUV(xy3)


        val vertices = FloatArray(20)

        val color = Color.WHITE_FLOAT_BITS

        vertices[0] = xy0.x
        vertices[1] = xy0.y
        vertices[2] = color
        vertices[3] = uv0.x
        vertices[4] = uv0.y

        vertices[5] = xy1.x
        vertices[6] = xy1.y
        vertices[7] = color
        vertices[8] = uv1.x
        vertices[9] = uv1.y

        vertices[10] = xy2.x
        vertices[11] = xy2.y
        vertices[12] = color
        vertices[13] = uv2.x
        vertices[14] = uv2.y

        vertices[15] = xy3.x
        vertices[16] = xy3.y
        vertices[17] = color
        vertices[18] = uv3.x
        vertices[19] = uv3.y


        batch!!.draw(
            texture,
            vertices,
            0,
            20
        )
    }

    var aiActLastTime = 0f

    val random = Random

    val stepTime = 0.5f

    var endingPlayer: EndingPlayer? = null

    var spinSpeed = when(difficulty) {
        Difficulty.Playable -> 0.0f
        Difficulty.Hard -> 0.1f
        Difficulty.Takodachi -> 0.6f
    }

    fun processInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            takodachi.dir = MotionDirection.UP
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            takodachi.dir = MotionDirection.DOWN
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            takodachi.dir = MotionDirection.LEFT
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            takodachi.dir = MotionDirection.RIGHT
        }
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            game.screen = TakoMenu(game)
        }
    }

    override fun resize(width: Int, height: Int) {
        cam.viewportWidth = width.toFloat()
        cam.viewportHeight = height.toFloat()
        cam.update(true)
        super.resize(width, height)

    }

    override fun hide() {
        music.stop()

    }

    fun aiAct(deltaTime: Float) {
        aiActLastTime += deltaTime
        if (aiActLastTime >= stepTime) {
            aiActLastTime = 0f

            mumeiis.forEach {
                it.animated.x = it.animated.tx
                it.animated.y = it.animated.ty

                var move = it.dir
                val directions = MotionDirection.values()

                val available = mutableListOf<MotionDirection>()

                for (i in -1..1) {
                    val dir = directions[(directions.indexOf(move) + i + 4) % 4]
                    if (!checkCollision(it.animated.x + dir.x, it.animated.y + dir.y)) {
                        available.add(dir)
                    }
                }
                move = if (available.isEmpty()) {
                    directions[(directions.indexOf(move) + 2) % 4]
                } else {
                    available.random(random)
                }

                it.animated.tx = it.animated.x + move.x
                it.animated.ty = it.animated.y + move.y
                it.animated.tState = 0f
                it.dir = move
            }

            takodachi.let { takodachi ->
                takodachi.animated.x = takodachi.animated.tx
                takodachi.animated.y = takodachi.animated.ty

                val cookie = cookies.find {
                    abs(it.x - takodachi.animated.x) <= 0.1f &&
                            abs(it.y - takodachi.animated.y) <= 0.1f
                }
                if (cookie != null) {
                    cookies.remove(cookie)
                    if (cookies.isEmpty()) {
                        resetMap()
                        spinSpeed *= 2f
                        music!!.stop()
                        winSound!!.play(0.1f)
                        endingPlayer = EndingPlayer(
                            win!!,
                            winSound!!,
                            5f
                        )
                    }
                }
                val mumeii = mumeiis.find {
                    abs(it.animated.x - takodachi.animated.x) <= 0.3f &&
                            abs(it.animated.y - takodachi.animated.y) <= 0.3f
                }
                if (mumeii != null) {
                    resetMap()
                    if (difficulty != Difficulty.Takodachi) {
                        spinSpeed /= 2f
                    }
                    music!!.stop()
                    loseSound!!.play(0.1f)
                    endingPlayer = EndingPlayer(
                        lose!!,
                        loseSound!!,
                        10f
                    )
                }

                val move = takodachi.dir
                if (!checkCollision(takodachi.animated.x + move.x, takodachi.animated.y + move.y)) {
                    takodachi.animated.tx = takodachi.animated.x + move.x
                    takodachi.animated.ty = takodachi.animated.y + move.y
                    takodachi.animated.tState = 0f
                }
            }
        }
    }

    fun updateAnimated(animated: Animated, deltaTime: Float) {
        animated.tState += 1f / stepTime * deltaTime
        animated.tState = animated.tState.coerceIn(0f, 1f)
    }

    fun renderAnimatedSprite(texture: Texture, animated: Animated) {
        animated.let {
            batch!!.draw(
                texture,
                -Gdx.graphics.width / 2f + (it.x * (1f - it.tState) + (it.tx) * it.tState) * 50f,
                -Gdx.graphics.height / 2f + (it.y * (1f - it.tState) + (it.ty) * it.tState) * 50f,
                50f,
                50f
            )
        }
    }

    fun animateAct(deltaTime: Float) {
        mumeiis.forEach {
            updateAnimated(it.animated, deltaTime)
        }
        updateAnimated(takodachi.animated, deltaTime)
    }

    private var rot = 0f

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 1f)

        rot += spinSpeed
        if (difficulty == Difficulty.Takodachi && abs(rot) > 180f) {
            spinSpeed = -spinSpeed
        }
        batch!!.projectionMatrix.set(cam.projection)
        batch!!.transformMatrix.set(cam.view)

        batch!!.begin()
        renderDBox(background!!, -Gdx.graphics.width.toFloat(), -Gdx.graphics.height.toFloat(), Gdx.graphics.width.toFloat() * 2f, Gdx.graphics.height.toFloat() * 2f, rot)
        blocks.forEach {
            renderDBox(walls!!, -Gdx.graphics.width / 2f + it.x * 50f, -Gdx.graphics.height/2f + it.y * 50f, it.size, it.size, -rot)
        }
        batch!!.end()
        batch!!.transformMatrix.rotate(Vector3.Z, rot)
        batch!!.begin()


        cookies.forEach {
            batch!!.draw(
                cookie,
                -Gdx.graphics.width / 2f + it.x * 50f, -Gdx.graphics.height/2f + it.y * 50f,
                50f, 50f
            )
        }

        mumeiis.forEach { char ->
            renderAnimatedSprite(mumeii!!, char.animated)
        }

        renderAnimatedSprite(tako!!, takodachi.animated)


        batch!!.end()

        batch!!.transformMatrix.rotate(Vector3.Z, -rot)

        if (endingPlayer != null) {
            endingPlayer!!.timeLeft -= delta
            batch!!.begin()
            batch!!.draw(endingPlayer!!.texture, -Gdx.graphics.width / 2f, -Gdx.graphics.height/2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            batch!!.end()
            if (endingPlayer!!.timeLeft < 0) {
                endingPlayer!!.sound.stop()
                endingPlayer = null
                music!!.play()
            }
        } else {
            animateAct(delta)
            aiAct(delta)

            processInput()

        }
    }



    override fun dispose() {
        batch!!.dispose()
        background!!.dispose()
        walls!!.dispose()
        mumeii!!.dispose()
        tako!!.dispose()
        cookie!!.dispose()
        win!!.dispose()
        lose!!.dispose()
        winSound!!.dispose()
        loseSound!!.dispose()
        music!!.dispose()
    }
}