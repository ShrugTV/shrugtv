package com.shrug.shrugtv.player

import com.shrug.shrugtv.model.Enums

class RemoteKeyBinding(
    val keyCodes: List<Int>,
    val isLongPress: Boolean,
    val action: Enums.RemoteKeyAction
) {
    companion object {
        // Without KEYCODE_MEDIA_PLAY (126): an explicit "Play" goes to the media session
        @JvmField
        val HANDLED_KEY_CODES: List<Int> = listOf(85, 90, 87, 89, 88, 82, 20, 21, 22, 167, 166)
    }
}
