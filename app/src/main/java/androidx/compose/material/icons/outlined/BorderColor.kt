/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.BorderColor: ImageVector
    get() {
        if (_borderColor != null) {
            return _borderColor!!
        }
        _borderColor = materialIcon(name = "Outlined.BorderColor") {
            materialPath {
                moveTo(16.81f, 8.94f)
                lineToRelative(-3.75f, -3.75f)
                lineTo(4.0f, 14.25f)
                verticalLineTo(18.0f)
                horizontalLineToRelative(3.75f)
                lineTo(16.81f, 8.94f)
                close()
                moveTo(6.0f, 16.0f)
                verticalLineToRelative(-0.92f)
                lineToRelative(7.06f, -7.06f)
                lineToRelative(0.92f, 0.92f)
                lineTo(6.92f, 16.0f)
                horizontalLineTo(6.0f)
                close()
            }
            materialPath {
                moveTo(19.71f, 6.04f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineToRelative(-2.34f, -2.34f)
                curveTo(17.17f, 2.09f, 16.92f, 2.0f, 16.66f, 2.0f)
                curveToRelative(-0.25f, 0.0f, -0.51f, 0.1f, -0.7f, 0.29f)
                lineToRelative(-1.83f, 1.83f)
                lineToRelative(3.75f, 3.75f)
                lineTo(19.71f, 6.04f)
                close()
            }
            materialPath {
                moveTo(2.0f, 20.0f)
                horizontalLineToRelative(20.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(-20.0f)
                close()
            }
        }
        return _borderColor!!
    }

private var _borderColor: ImageVector? = null
