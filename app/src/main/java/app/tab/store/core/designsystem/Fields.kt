package app.tab.store.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import app.tab.store.core.ui.OutlinedTextField
import app.tab.store.core.ui.OutlinedTextFieldDefaults
import app.tab.store.core.ui.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SproutlyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    prefix: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(label, style = Tokens.typography.bodyMedium) },
            placeholder = placeholder?.let {
                { Text(it, style = Tokens.typography.bodyMedium) }
            },
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            shape = Tokens.shapes.medium,
            textStyle = Tokens.typography.bodyLarge,
            prefix = prefix?.let {
                { Text(it, style = Tokens.typography.bodyLarge) }
            },
            trailingIcon = trailing,
            visualTransformation = when {
                !isPassword -> VisualTransformation.None
                passwordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Tokens.colorScheme.primary,
                unfocusedBorderColor = Tokens.colorScheme.outline,
                focusedLabelColor = Tokens.colorScheme.primary,
                cursorColor = Tokens.colorScheme.primary,
                focusedContainerColor = Tokens.colorScheme.surface,
                unfocusedContainerColor = Tokens.colorScheme.surface,
            ),
        )
        val note = when {
            isError && errorText != null -> errorText
            helperText != null -> helperText
            else -> null
        }
        if (note != null) {
            Text(
                text = note,
                style = Tokens.typography.bodySmall,
                color = if (isError) Tokens.colorScheme.error
                else Tokens.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp, top = 5.dp),
            )
        }
        Spacer(Modifier.height(if (note != null) 8.dp else 14.dp))
    }
}
