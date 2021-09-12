package tech.smallwonder.simplemusic.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SearchBar(showBack: Boolean = true, controller: NavHostController, text: String, onTextChange: (value: String) -> Unit, onClearSelected: () -> Unit, searchHint: String, showMore: Boolean = true, onMoreSelected: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        if (showBack) {
            IconButton(onClick = {
                controller.popBackStack()
            }) {
                Icon(
                    tint = Color.Gray,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }

        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            trailingIcon = if (text.isNotEmpty()) {{
                IconButton(
                    onClick = onClearSelected,
                ) {
                    Icon(
                        modifier = Modifier
                            .background(
                                Color(0, 0, 0, 255 / 2),
                                RoundedCornerShape(8.dp)
                            ),
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                    )
                }
            }} else null,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                disabledTextColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(searchHint)
            }
        )

        if (showMore) {
            MediaIcon(
                vector = Icons.Filled.MoreVert,
                onClick = onMoreSelected,
            )
        }
    }
}