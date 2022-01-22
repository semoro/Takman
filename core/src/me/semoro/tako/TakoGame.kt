package me.semoro.tako

import com.badlogic.gdx.Game


class TakoGame : Game() {
    override fun create() {
        setScreen(TakoMenu(this))
    }
}