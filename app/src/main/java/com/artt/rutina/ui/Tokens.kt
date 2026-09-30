package com.artt.rutina.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Единая система радиусов. Компоненты не задают скругление сами — иначе surfaces
 * начинают «расходиться»: карточка 18, меню 4, поле 4, шит 24 — это и читается как
 * несогласованность. Меняем здесь — меняется везде.
 */
internal object Radius {
    val card = RoundedCornerShape(16.dp)
    val menu = RoundedCornerShape(12.dp)

    /**
     * Один радиус на все управляющие элементы — чипы, кнопки, поля, степпер.
     * Раньше они жили каждый со своим скруглением, и разница читалась как
     * несобранность: интерфейс выглядел так, будто собран из разных наборов.
     */
    val field = RoundedCornerShape(12.dp)
    val fab = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val cell = RoundedCornerShape(4.dp)
    val segment = RoundedCornerShape(3.dp)

    /** Промежуток между ячейками календаря. */
    val gridGap = 4.dp
}

/** Единая шкала отступов, чтобы вертикальный ритм не «прыгал» между экранами. */
internal object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val screen = 14.dp
}

/** Общие размеры иконок: 20 — действие, 12 — подпись. */
internal object IconSize {
    val action = 20.dp
    val caption = 12.dp
    val status = 22.dp
}

/**
 * Один вид Switch на всё приложение: M3 по умолчанию — трек в состоянии off
 * слишком тёмный и рисует вокруг себя тяжёлую рамку, которая выбивается из лёгкого
 * интерфейса. Здесь off — приглушённая заливка без обводки, on — обычный акцент.
 */
@Composable
internal fun rutinaSwitchColors() = SwitchDefaults.colors(
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = Color.Transparent,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedIconColor = Color.Transparent,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedIconColor = MaterialTheme.colorScheme.onPrimary,
)

