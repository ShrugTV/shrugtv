package com.shrug.shrugtv.events

class AppEvent(val type: Type) {
    enum class Type {
        REFRESH,
        SETTINGS,
        AUTO_UPDATE,
        REFRESH_STREAMCATEGORY,
        FAVORITE_CHANGED,
        CONTEXT_MENU,
        TEST,
        FOLLOW_CHANNEL_CHANGED,
        FOLLOW_GAME_CHANGED
    }
}
