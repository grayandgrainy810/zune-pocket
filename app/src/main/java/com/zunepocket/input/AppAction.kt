package com.zunepocket.input

/** What the user meant, independent of which button or gesture produced it. */
enum class AppAction {
    Up, Down, Left, Right,
    Confirm, Back,
    NowPlaying, Context,
    PageUp, PageDown,
    PrevGroup, NextGroup,
    PlayPause, JumpList,
}
