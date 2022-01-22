package me.semoro.tako

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array as GdxArray

enum class Difficulty {
    Playable,
    Hard,
    Takodachi
}

class TakoMenu(private val game: TakoGame) : ScreenAdapter() {

    val music = Gdx.audio.newMusic(Gdx.files.internal("menu-music.mp3")).apply {
        isLooping = true
        volume = 0.1f
        play()
    }

    private val background = Texture("background.png")
    private val walls = Texture("walls.png")

    private val menuTako = Texture("menu-tako.png")
    private val menuLevel0 = Texture("menu_btn0.png")
    private val menuLevel1 = Texture("menu_btn1.png")
    private val menuLevel2 = Texture("menu_btn2.png")


    private val menuTakoFrames = run {
        val frames = TextureRegion.split(menuTako, 498, 485)
        val array = GdxArray<TextureRegion>()
        for (line in frames) {
            for (f in line) {
                array.add(f)
            }
        }
        Animation<TextureRegion>(0.066f, array, Animation.PlayMode.LOOP)
    }

    val batch = SpriteBatch()

    override fun show() {
        music.play()
    }

    override fun hide() {
        music.stop()
    }


    private fun onMenuButtonPressed(id: Difficulty) {
        game.screen = TakoGameScreen(game, id)
    }

    var stateTime = 0f

    private fun drawButtonUnderlay(texture: Texture, x: Float, y: Float, id: Difficulty) {

        val mx = Gdx.input.x
        val my = Gdx.graphics.height - Gdx.input.y

        val u = x / Gdx.graphics.width
        val v = y / Gdx.graphics.height

        if (
            x <= mx && mx <= (x + texture.width) &&
            y <= my && my <=(y + texture.height)
        ) {
            batch.draw(
                walls, x, y, texture.width.toFloat(), texture.height.toFloat(),
                u, 1f - v,
                u + texture.width.toFloat() / Gdx.graphics.width, 1f - (v + texture.height.toFloat() / Gdx.graphics.height)
            )
            if (Gdx.input.isButtonPressed(0)) {
                onMenuButtonPressed(id)
            }
        }
    }

    override fun render(delta: Float) {
        super.render(delta)
        stateTime += delta

        batch.begin()
        batch.draw(background, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        val region = menuTakoFrames.getKeyFrame(stateTime)

        drawButtonUnderlay(menuLevel0, 10f, 110f, Difficulty.Playable)
        drawButtonUnderlay(menuLevel1, 10f, 60f, Difficulty.Hard)
        drawButtonUnderlay(menuLevel2, 10f, 10f, Difficulty.Takodachi)

        batch.draw(region, Gdx.graphics.width.toFloat() / 2f - region.regionWidth / 2f, Gdx.graphics.height.toFloat() / 2f - region.regionHeight / 2f)

        batch.draw(menuLevel0, 10f, 110f)
        batch.draw(menuLevel1, 10f, 60f)
        batch.draw(menuLevel2, 10f, 10f)

        batch.end()
    }

    override fun dispose() {
        music.dispose()
        batch.dispose()
        menuTako.dispose()
        menuLevel0.dispose()
        menuLevel1.dispose()
        menuLevel2.dispose()
        background.dispose()
        walls.dispose()
    }
}