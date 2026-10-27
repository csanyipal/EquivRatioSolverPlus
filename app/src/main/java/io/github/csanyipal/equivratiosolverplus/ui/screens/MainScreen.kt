// Copyright 2026 Csányi Pál
//
// This file is part of EquivRatioSolverPlus.
//
// EquivRatioSolverPlus is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
// EquivRatioSolverPlus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along with EquivRatioSolverPlus. If not, see <https://www.gnu.org/licenses/>.

package io.github.csanyipal.equivratiosolverplus.ui.screens

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.Canvas
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.csanyipal.equivratiosolverplus.R
import io.github.csanyipal.equivratiosolverplus.MainActivity
import io.github.csanyipal.equivratiosolverplus.data.SavedRatio
import io.github.csanyipal.equivratiosolverplus.data.SavedRatiosDatabase

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val inputScrollState = rememberScrollState()
    var focusedInputIndex by remember { mutableStateOf<Int?>(null) }
    val inputScrollScope = rememberCoroutineScope()
    LaunchedEffect(isKeyboardVisible, focusedInputIndex) {
        if (isKeyboardVisible) {
            withFrameNanos { }
            inputScrollState.animateScrollTo(inputScrollState.maxValue)
        }
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var pendingLanguage by remember {
        mutableStateOf(
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .getString(LANGUAGE_KEY, "en") ?: "en"
        )
    }
    var privacyDialogVisible by remember { mutableStateOf(false) }
    var softwareLicenseDialogVisible by remember { mutableStateOf(false) }
    var aboutDialogVisible by remember { mutableStateOf(false) }
    var donationDialogVisible by remember { mutableStateOf(false) }
    var closeApplicationDialogVisible by remember { mutableStateOf(false) }
    var startupNoticeVisible by remember {
        mutableStateOf(
            !context.getSharedPreferences(
                PREFERENCES_NAME,
                Activity.MODE_PRIVATE
            ).getBoolean(LICENSE_NOTICE_DISMISSED_KEY, false)
        )
    }
    var suppressStartupNotice by remember { mutableStateOf(false) }
    var inverseProportionality by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .getBoolean(INVERSE_PROPORTIONALITY_KEY, false)
        )
    }
    var decimalPlaces by remember {
        mutableStateOf(
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .getInt(DECIMAL_PLACES_KEY, 3).toString()
        )
    }
    var angleUnit by remember {
        mutableStateOf(
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .getString(ANGLE_UNIT_KEY, AngleUnit.DEGREES.name) ?: AngleUnit.DEGREES.name
        )
    }
    var helpHidden by remember {
        mutableStateOf(
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .getBoolean(HELP_HIDDEN_KEY, false)
        )
    }
    var y1 by rememberSaveable { mutableStateOf("") }
    var x1 by rememberSaveable { mutableStateOf("") }
    var y2 by rememberSaveable { mutableStateOf("") }
    var x2 by rememberSaveable { mutableStateOf("") }
    var yComments by rememberSaveable { mutableStateOf("") }
    var xComments by rememberSaveable { mutableStateOf("") }
    var calculatedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingConfirmation by remember { mutableStateOf<ConfirmationAction?>(null) }
    var selectedScreen by remember { mutableStateOf(0) }
    var operationVariableIndex by rememberSaveable { mutableStateOf(0) }
    var operationSymbolIndex by rememberSaveable { mutableStateOf(0) }
    var operationOperand by rememberSaveable { mutableStateOf("") }
    var operationNote by rememberSaveable { mutableStateOf("") }
    var manualDialogVisible by remember { mutableStateOf(false) }
    var theoryDialogVisible by remember { mutableStateOf(false) }
    var storeDialogVisible by remember { mutableStateOf(false) }
    var restoreDialogVisible by remember { mutableStateOf(false) }
    var storedRatioName by rememberSaveable { mutableStateOf("") }
    var savedRatios by remember { mutableStateOf(emptyList<SavedRatio>()) }
    val savedRatiosDatabase = remember(context) {
        SavedRatiosDatabase(context.applicationContext)
    }
    val values = listOf(y1, x1, y2, x2)
    val storeNotCalculatedMessage = stringResource(R.string.store_ratio_not_calculated)
    val storedMessage = stringResource(R.string.store_ratio_saved)
    val duplicateNameMessage = stringResource(R.string.store_ratio_name_exists)
    val nameRequiredMessage = stringResource(R.string.store_ratio_name_required)
    val noSavedRatiosMessage = stringResource(R.string.saved_ratios_empty)
    val storeFailureMessage = stringResource(R.string.operation_save_failed)
    val focusRequesters = remember { List(4) { FocusRequester() } }
    val bringIntoViewRequesters = remember { List(4) { BringIntoViewRequester() } }
    fun toggleProportionality() {
        inverseProportionality = !inverseProportionality
        context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
            .edit()
            .putBoolean(INVERSE_PROPORTIONALITY_KEY, inverseProportionality)
            .apply()
    }
    fun setInput(index: Int, value: String) {
        when (index) {
            0 -> y1 = value
            1 -> x1 = value
            2 -> y2 = value
            else -> x2 = value
        }
    }
    fun updateInput(index: Int, value: String) {
        setInput(index, value)
        val missingIndex = calculatedIndex
        if (missingIndex != null && index != missingIndex) {
            val values = listOf(y1, x1, y2, x2).toMutableList()
            values[index] = value
            val recalculated = calculateMissingValue(
                values,
                missingIndex,
                inverseProportionality
            )
            if (recalculated != null) {
                setInput(missingIndex, recalculated)
            } else {
                setInput(missingIndex, "")
                calculatedIndex = null
            }
        }
    }
    fun handleDone(index: Int) {
        val values = listOf(y1, x1, y2, x2)
        if (calculatedIndex != null && values.all { it.isNotBlank() }) {
            keyboardController?.hide()
            return
        }
        val emptyIndices = values.mapIndexedNotNull { valueIndex, value ->
            valueIndex.takeIf { value.isBlank() }
        }
        val calculated = if (values[index].isNotBlank() && emptyIndices.size == 1) {
            calculateMissingValue(values, emptyIndices.single(), inverseProportionality)
        } else {
            null
        }
        if (calculated != null) {
            setInput(emptyIndices.single(), calculated)
            calculatedIndex = emptyIndices.single()
            keyboardController?.hide()
        } else {
            focusRequesters[(index + 1) % 4].requestFocus()
        }
    }
    fun refreshSavedRatios() {
        try {
            savedRatios = savedRatiosDatabase.getAll()
            restoreDialogVisible = true
        } catch (_: SQLiteException) {
            Toast.makeText(context, storeFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }
    fun restoreRatio(ratio: SavedRatio) {
        inverseProportionality = ratio.inverse
        context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
            .edit()
            .putBoolean(INVERSE_PROPORTIONALITY_KEY, ratio.inverse)
            .apply()
        setInput(0, ratio.values[0])
        setInput(1, ratio.values[1])
        setInput(2, ratio.values[2])
        setInput(3, ratio.values[3])
        calculatedIndex = ratio.calculatedIndex
        yComments = ratio.yComments
        xComments = ratio.xComments
        operationVariableIndex = ratio.operationVariableIndex
        operationSymbolIndex = ratio.operationIndex
        operationOperand = ratio.operationOperand
        operationNote = ratio.operationNote
        restoreDialogVisible = false
    }
    val activity = context as? Activity
    val mainActivity = context as? MainActivity

    fun dismissStartupNotice() {
        if (suppressStartupNotice) {
            context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putBoolean(LICENSE_NOTICE_DISMISSED_KEY, true)
                .apply()
        }
        startupNoticeVisible = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        MenuItem(R.string.menu_manual) {
                            menuExpanded = false
                            manualDialogVisible = true
                        }
                        MenuItem(R.string.menu_theory) {
                            menuExpanded = false
                            theoryDialogVisible = true
                        }
                        MenuItem(R.string.menu_settings) {
                            menuExpanded = false
                            settingsDialogVisible = true
                        }
                        MenuItem(R.string.menu_privacy_policies) {
                            menuExpanded = false
                            privacyDialogVisible = true
                        }
                        MenuItem(R.string.menu_software_license) {
                            menuExpanded = false
                            softwareLicenseDialogVisible = true
                        }
                        MenuItem(R.string.menu_donation) {
                            menuExpanded = false
                            donationDialogVisible = true
                        }
                        MenuItem(R.string.menu_about) {
                            menuExpanded = false
                            aboutDialogVisible = true
                        }
                        MenuItem(R.string.menu_stop_app) {
                            menuExpanded = false
                            closeApplicationDialogVisible = true
                        }
                    }

                    if (closeApplicationDialogVisible) {
                        AlertDialog(
                            onDismissRequest = { closeApplicationDialogVisible = false },
                            title = {
                                Text(text = stringResource(R.string.close_application_title))
                            },
                            text = {
                                Text(text = stringResource(R.string.close_application_message))
                            },
                            dismissButton = {
                                Button(
                                    onClick = { closeApplicationDialogVisible = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = stringResource(R.string.cancel))
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        closeApplicationDialogVisible = false
                                        activity?.finishAffinity()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = stringResource(R.string.yes))
                                }
                            }
                        )
                    }

                    if (settingsDialogVisible) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text(text = stringResource(R.string.settings_title)) },
                            text = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_graph_decimal_places),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    OutlinedTextField(
                                        value = decimalPlaces,
                                        onValueChange = { value ->
                                            if (value.all(Char::isDigit) && value.length <= 2) {
                                                decimalPlaces = value
                                                value.toIntOrNull()?.coerceIn(0, 9)?.let {
                                                    context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                                                        .edit().putInt(DECIMAL_PLACES_KEY, it).apply()
                                                }
                                            }
                                        },
                                        label = {
                                            Text(stringResource(R.string.settings_decimal_places))
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_angle_format),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    listOf(
                                        AngleUnit.DEGREES,
                                        AngleUnit.DEGREES_MINUTES,
                                        AngleUnit.DEGREES_MINUTES_SECONDS,
                                        AngleUnit.RADIANS
                                    )
                                        .forEach { unit ->
                                            Button(
                                                onClick = {
                                                    angleUnit = unit.name
                                                    context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                                                        .edit().putString(ANGLE_UNIT_KEY, unit.name).apply()
                                                }
                                            ) {
                                                val unitLabel = stringResource(unit.labelResource)
                                                Text(
                                                    if (angleUnit == unit.name) {
                                                        "✓ $unitLabel"
                                                    } else {
                                                        unitLabel
                                                    }
                                                )
                                            }
                                        }
                                        Text(
                                            text = stringResource(R.string.language_settings),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Button(
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        onClick = {
                                            pendingLanguage = "en"
                                        }
                                    ) {
                                        Text(
                                            text = if (pendingLanguage == "en") {
                                                "✓ ${stringResource(R.string.language_english)}"
                                            } else {
                                                stringResource(R.string.language_english)
                                            }
                                        )
                                    }
                                    Button(
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        onClick = {
                                            pendingLanguage = "hu"
                                        }
                                    ) {
                                        Text(
                                            text = if (pendingLanguage == "hu") {
                                                "✓ ${stringResource(R.string.language_hungarian)}"
                                            } else {
                                                stringResource(R.string.language_hungarian)
                                            }
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        settingsDialogVisible = false
                                        mainActivity?.setAppLanguage(pendingLanguage)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(stringResource(R.string.startup_notice_acknowledge))
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 },
                    icon = { Text("⌨") },
                    label = { Text(stringResource(R.string.navigation_data)) }
                )
                NavigationBarItem(
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 },
                    icon = { Text("📈") },
                    label = { Text(stringResource(R.string.navigation_graph)) }
                )
                NavigationBarItem(
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2 },
                    icon = { Text("☷") },
                    label = { Text(stringResource(R.string.navigation_result)) }
                )
                NavigationBarItem(
                    selected = selectedScreen == 3,
                    onClick = { selectedScreen = 3 },
                    icon = { Text("±") },
                    label = { Text(stringResource(R.string.navigation_operation)) }
                )
            }
        }
    ) {
        innerPadding ->
        if (selectedScreen == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .imePadding()
                    .verticalScroll(inputScrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            if (!isKeyboardVisible) {
            Button(
                onClick = {
                    if (listOf(y1, x1, y2, x2).all { it.isBlank() }) {
                        toggleProportionality()
                    } else {
                        pendingConfirmation = ConfirmationAction.ChangeProportionality
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(R.string.change_proportionality_type))
                }
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = stringResource(
                    if (inverseProportionality) {
                        R.string.inverse_proportionality
                    } else {
                        R.string.direct_proportionality
                    }
                ),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (inverseProportionality) {
                    "y ∝ k : x, k = y₁ ⋅ x₁ = y₂ ⋅ x₂"
                } else {
                    "y ∝ k ⋅ x, k = y₁ : x₁ = y₂ : x₂"
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = helpHidden,
                    onCheckedChange = { hidden ->
                        helpHidden = hidden
                        context.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)
                            .edit()
                            .putBoolean(HELP_HIDDEN_KEY, hidden)
                            .apply()
                    }
                )
                Text(text = stringResource(R.string.hide_help_description))
            }
            if (!helpHidden) {
                Text(
                    text = stringResource(R.string.short_help),
                    color = if (isSystemInDarkTheme()) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        Color(0xFF00695C)
                    }
                )
            }
            Text(
                text = stringResource(R.string.entering_data_and_comments),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
            }
            val fields = listOf(
                Triple(y1, R.string.y1_hint, "y₁"),
                Triple(x1, R.string.x1_hint, "x₁"),
                Triple(y2, R.string.y2_hint, "y₂"),
                Triple(x2, R.string.x2_hint, "x₂")
            )
            fields.forEachIndexed { index, (value, hintResource, variableLabel) ->
                InputRow(
                    value = value,
                    onValueChange = { updateInput(index, it) },
                    readOnly = calculatedIndex == index,
                    hint = stringResource(hintResource),
                    variableLabel = variableLabel,
                    focusRequester = focusRequesters[index],
                    bringIntoViewRequester = bringIntoViewRequesters[index],
                    onFocus = {
                        focusedInputIndex = index
                        inputScrollScope.launch {
                            delay(300)
                            bringIntoViewRequesters[index].bringIntoView()
                        }
                    },
                    onDone = { handleDone(index) },
                    onDelete = {
                        if (calculatedIndex != index) {
                            setInput(index, "")
                        }
                    }
                )
                Spacer(modifier = Modifier.padding(2.dp))
            }
            if (!isKeyboardVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (calculatedIndex == null || values.any { it.toDoubleOrNull() == null }) {
                                Toast.makeText(
                                    context,
                                    storeNotCalculatedMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                storedRatioName = ""
                                storeDialogVisible = true
                            }
                        }
                    ) {
                        Text(stringResource(R.string.store_button))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (values.any { it.isNotBlank() }) {
                                pendingConfirmation = ConfirmationAction.Restore
                            } else {
                                refreshSavedRatios()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.restore_button))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (values.any { it.isNotBlank() }) {
                                pendingConfirmation = ConfirmationAction.ClearAll
                            }
                        }
                    ) {
                        Text(stringResource(R.string.delete_all_button))
                    }
                }
            }
            }
        } else if (selectedScreen == 1) {
            GraphScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                inverse = inverseProportionality,
                values = listOf(y1, x1, y2, x2),
                decimalPlaces = decimalPlaces.toIntOrNull()?.coerceIn(0, 9) ?: 3,
                angleUnit = angleUnit
            )
        } else if (selectedScreen == 2) {
            ResultScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                inverse = inverseProportionality,
                values = listOf(y1, x1, y2, x2),
                yComments = yComments,
                xComments = xComments,
                onYCommentsChange = { yComments = it },
                onXCommentsChange = { xComments = it },
                decimalPlaces = decimalPlaces.toIntOrNull()?.coerceIn(0, 9) ?: 3,
                angleUnit = angleUnit
            )
        } else {
            OperationScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                inverse = inverseProportionality,
                values = listOf(y1, x1, y2, x2),
                decimalPlaces = decimalPlaces.toIntOrNull()?.coerceIn(0, 9) ?: 3,
                yComments = yComments,
                xComments = xComments,
                variableIndex = operationVariableIndex,
                onVariableIndexChange = { operationVariableIndex = it },
                operationIndex = operationSymbolIndex,
                onOperationIndexChange = { operationSymbolIndex = it },
                operand = operationOperand,
                onOperandChange = { operationOperand = it },
                note = operationNote,
                onNoteChange = { operationNote = it }
            )
        }
    }

    if (startupNoticeVisible) {
        AlertDialog(
            onDismissRequest = ::dismissStartupNotice,
            title = { Text(text = stringResource(R.string.startup_notice_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.startup_notice_message))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = suppressStartupNotice,
                            onCheckedChange = { suppressStartupNotice = it }
                        )
                        Text(text = stringResource(R.string.startup_notice_do_not_show_again))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = ::dismissStartupNotice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.startup_notice_acknowledge))
                }
            }
        )
    }

    if (pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text(text = stringResource(R.string.attention)) },
            text = { Text(text = stringResource(R.string.lose_proportionality_warning)) },
            dismissButton = {
                Button(
                    onClick = { pendingConfirmation = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (pendingConfirmation) {
                            ConfirmationAction.ClearAll -> {
                                y1 = ""
                                x1 = ""
                                y2 = ""
                                x2 = ""
                                calculatedIndex = null
                            }
                            ConfirmationAction.ChangeProportionality -> {
                                y1 = ""
                                x1 = ""
                                y2 = ""
                                x2 = ""
                                calculatedIndex = null
                                toggleProportionality()
                            }
                            ConfirmationAction.Restore -> refreshSavedRatios()
                            null -> Unit
                        }
                        pendingConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.yes))
                }
            }
        )
    }

    if (storeDialogVisible) {
        AlertDialog(
            onDismissRequest = { storeDialogVisible = false },
            title = { Text(stringResource(R.string.store_ratio_title)) },
            text = {
                OutlinedTextField(
                    value = storedRatioName,
                    onValueChange = { storedRatioName = it },
                    label = { Text(stringResource(R.string.store_ratio_name_hint)) },
                    singleLine = true
                )
            },
            dismissButton = {
                Button(onClick = { storeDialogVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = storedRatioName.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, nameRequiredMessage, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val result = calculateOperationResult(
                            values,
                            operationVariableIndex,
                            operationSymbolIndex,
                            operationOperand
                        )
                        try {
                            savedRatiosDatabase.insert(
                                SavedRatio(
                                    name = name,
                                    inverse = inverseProportionality,
                                    values = values,
                                    calculatedIndex = calculatedIndex ?: return@Button,
                                    yComments = yComments,
                                    xComments = xComments,
                                    operationVariableIndex = operationVariableIndex,
                                    operationIndex = operationSymbolIndex,
                                    operationOperand = operationOperand,
                                    operationResult = result,
                                    operationNote = operationNote
                                )
                            )
                            storeDialogVisible = false
                            Toast.makeText(context, storedMessage, Toast.LENGTH_SHORT).show()
                        } catch (_: SQLiteConstraintException) {
                            Toast.makeText(
                                context,
                                duplicateNameMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: SQLiteException) {
                            Toast.makeText(
                                context,
                                storeFailureMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.store_button))
                }
            }
        )
    }

    if (restoreDialogVisible) {
        AlertDialog(
            onDismissRequest = { restoreDialogVisible = false },
            title = { Text(stringResource(R.string.saved_ratios_title)) },
            text = {
                if (savedRatios.isEmpty()) {
                    Text(noSavedRatiosMessage)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        savedRatios.forEach { ratio ->
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { restoreRatio(ratio) }
                            ) {
                                Text(ratio.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { restoreDialogVisible = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (manualDialogVisible) {
        val manualDarkTheme = isSystemInDarkTheme()
        Dialog(
            onDismissRequest = { manualDialogVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(manualDarkTheme) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { viewContext ->
                                WebView(viewContext).apply {
                                    settings.javaScriptEnabled = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        @Suppress("DEPRECATION")
                                        settings.forceDark = WebSettings.FORCE_DARK_OFF
                                    }
                                    webViewClient = WebViewClient()
                                    val language = viewContext.getSharedPreferences(
                                        PREFERENCES_NAME,
                                        Activity.MODE_PRIVATE
                                    ).getString(
                                        LANGUAGE_KEY,
                                        viewContext.resources.configuration.locales[0].language
                                    )
                                    val assetName = if (language == "hu") {
                                        "manual_hu.html"
                                    } else {
                                        "manual_en.html"
                                    }
                                    val themeCss = if (manualDarkTheme) {
                                        """
                                        <style>
                                          html, body {
                                            color-scheme: dark !important;
                                            color: #f2f2f2 !important;
                                            background-color: #121212 !important;
                                          }
                                          html body * { color: #f2f2f2 !important; }
                                        </style>
                                        """.trimIndent()
                                    } else {
                                        """
                                        <style>
                                          html, body {
                                            color-scheme: light !important;
                                            color: #000000 !important;
                                            background-color: #ffffff !important;
                                          }
                                          html body * { color: #000000 !important; }
                                        </style>
                                        """.trimIndent()
                                    }
                                    val html = viewContext.assets.open(assetName)
                                        .bufferedReader()
                                        .use { it.readText() }
                                        .replace(
                                            "</head>",
                                            "$themeCss</head>",
                                            ignoreCase = true
                                        )
                                    loadDataWithBaseURL(
                                        "file:///android_asset/",
                                        html,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        )
                    }
                    Button(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        onClick = { manualDialogVisible = false }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }

    if (theoryDialogVisible) {
        val theoryDarkTheme = isSystemInDarkTheme()
        Dialog(
            onDismissRequest = { theoryDialogVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(theoryDarkTheme) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { viewContext ->
                                WebView(viewContext).apply {
                                    settings.javaScriptEnabled = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        @Suppress("DEPRECATION")
                                        settings.forceDark = WebSettings.FORCE_DARK_OFF
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest
                                        ): Boolean {
                                            val uri = request.url
                                            return if (
                                                uri.scheme == "http" || uri.scheme == "https"
                                            ) {
                                                viewContext.startActivity(
                                                    Intent(Intent.ACTION_VIEW, uri)
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                    val language = viewContext.getSharedPreferences(
                                        PREFERENCES_NAME,
                                        Activity.MODE_PRIVATE
                                    ).getString(
                                        LANGUAGE_KEY,
                                        viewContext.resources.configuration.locales[0].language
                                    )
                                    val assetName = if (language == "hu") {
                                        "theory_hu.html"
                                    } else {
                                        "theory_en.html"
                                    }
                                    val themeCss = if (theoryDarkTheme) {
                                        """
                                        <style>
                                          html, body {
                                            color-scheme: dark !important;
                                            color: #f2f2f2 !important;
                                            background-color: #121212 !important;
                                          }
                                          html body * { color: #f2f2f2 !important; }
                                          html body div[class],
                                          html body div[class] * { color: #000000 !important; }
                                        </style>
                                        """.trimIndent()
                                    } else {
                                        """
                                        <style>
                                          html, body {
                                            color-scheme: light !important;
                                            color: #000000 !important;
                                            background-color: #ffffff !important;
                                          }
                                          html body * { color: #000000 !important; }
                                        </style>
                                        """.trimIndent()
                                    }
                                    val html = viewContext.assets.open(assetName)
                                        .bufferedReader()
                                        .use { it.readText() }
                                        .replace(
                                            "</head>",
                                            "$themeCss</head>",
                                            ignoreCase = true
                                        )
                                    loadDataWithBaseURL(
                                        "file:///android_asset/",
                                        html,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        )
                    }
                    Button(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        onClick = { theoryDialogVisible = false }
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }

    if (privacyDialogVisible) {
        AlertDialog(
            onDismissRequest = { privacyDialogVisible = false },
            title = { Text(text = stringResource(R.string.menu_privacy_policies)) },
            text = {
                LegalText(
                    text = stringResource(R.string.privacy_policy),
                    headings = listOf(
                        stringResource(R.string.privacy_policy_heading),
                        stringResource(R.string.privacy_policy_introduction),
                        stringResource(R.string.privacy_policy_data_collection),
                        stringResource(R.string.privacy_policy_data_sharing),
                        stringResource(R.string.privacy_policy_contact),
                        stringResource(R.string.privacy_policy_changes)
                    ),
                    onEmailClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse(
                                    "mailto:scaynialp@gmail.com?subject=${
                                        Uri.encode("Equiv Ratio Solver Plus - ")
                                    }"
                                )
                            )
                        )
                    },
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                Button(
                    onClick = { privacyDialogVisible = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.startup_notice_acknowledge))
                }
            }
        )
    }

    if (softwareLicenseDialogVisible) {
        AlertDialog(
            onDismissRequest = { softwareLicenseDialogVisible = false },
            title = { Text(text = stringResource(R.string.menu_software_license)) },
            text = {
                LegalText(
                    text = stringResource(R.string.privacy_policy_notice),
                    headings = emptyList(),
                    onLicenseLinkClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.gnu.org/licenses/")
                            )
                        )
                    },
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                Button(
                    onClick = { softwareLicenseDialogVisible = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.startup_notice_acknowledge))
                }
            }
        )
    }

    if (aboutDialogVisible) {
        AlertDialog(
            onDismissRequest = { aboutDialogVisible = false },
            title = { Text(text = stringResource(R.string.about_title)) },
            text = { Text(text = stringResource(R.string.about_message)) },
            confirmButton = {
                Button(
                    onClick = { aboutDialogVisible = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.startup_notice_acknowledge))
                }
            }
        )
    }

    if (donationDialogVisible) {
        AlertDialog(
            onDismissRequest = { donationDialogVisible = false },
            title = { Text(text = stringResource(R.string.donation_title)) },
            text = { Text(text = stringResource(R.string.donation_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        donationDialogVisible = false
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://sites.google.com/view/scaynialp/home/donation")
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.donation_button))
                }
            }
        )
    }
}

private enum class ConfirmationAction {
    ClearAll,
    ChangeProportionality,
    Restore
}

private const val PREFERENCES_NAME = "app_preferences"
private const val LANGUAGE_KEY = "language"
private const val LICENSE_NOTICE_DISMISSED_KEY = "license_notice_dismissed"
private const val HELP_HIDDEN_KEY = "help_hidden"
private const val INVERSE_PROPORTIONALITY_KEY = "inverse_proportionality"
private const val DECIMAL_PLACES_KEY = "graph_decimal_places"
private const val ANGLE_UNIT_KEY = "graph_angle_unit"

private enum class AngleUnit(val labelResource: Int) {
    DEGREES(R.string.settings_angle_degrees),
    DEGREES_MINUTES(R.string.settings_angle_degrees_minutes),
    DEGREES_MINUTES_SECONDS(R.string.settings_angle_degrees_minutes_seconds),
    RADIANS(R.string.settings_angle_radians)
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
private fun OperationScreen(
    modifier: Modifier,
    inverse: Boolean,
    values: List<String>,
    decimalPlaces: Int,
    yComments: String,
    xComments: String,
    variableIndex: Int,
    onVariableIndexChange: (Int) -> Unit,
    operationIndex: Int,
    onOperationIndexChange: (Int) -> Unit,
    operand: String,
    onOperandChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit
) {
    val context = LocalContext.current
    val noteFocusRequester = remember { FocusRequester() }
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val operationScope = rememberCoroutineScope()
    val directTitle = stringResource(R.string.direct_proportionality)
    val inverseTitle = stringResource(R.string.inverse_proportionality)
    val sendLabel = stringResource(R.string.operation_send)
    val saveLabel = stringResource(R.string.operation_save)
    val savedMessage = stringResource(R.string.operation_saved)
    val saveFailedMessage = stringResource(R.string.operation_save_failed)
    val variableLabels = listOf("y₁", "x₁", "y₂", "x₂")
    val operations = listOf("+", "-", "⋅", ":")
    var variableMenuExpanded by remember { mutableStateOf(false) }
    var operationMenuExpanded by remember { mutableStateOf(false) }
    val operationResult = calculateOperationResult(
        values,
        variableIndex,
        operationIndex,
        operand
    )
    val displayedValues = values.map { formatVariableValue(it, decimalPlaces) }
    val hasCalculatedValues = values.all { it.toDoubleOrNull() != null }
    val resultText = operationResult?.let {
        String.format(Locale.US, "%.${decimalPlaces.coerceIn(0, 9)}f", it)
    } ?: ""
    val report = buildOperationReport(
        values = values,
        yComments = yComments,
        xComments = xComments,
        variableIndex = variableIndex,
        operation = operations[operationIndex],
        operand = operand,
        result = resultText,
        note = note,
        title = if (inverse) inverseTitle else directTitle
    )

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.operation_title),
            style = MaterialTheme.typography.titleLarge
        )
        values.forEachIndexed { index, _ ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${variableLabels[index]} = ${displayedValues[index]}",
                    modifier = Modifier.weight(1f),
                    color = if (index == variableIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                val variableNote = if (index == 0 || index == 2) yComments else xComments
                if (variableNote.isNotBlank()) {
                    Text(
                        text = variableNote,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        if (hasCalculatedValues) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(onClick = { variableMenuExpanded = true }) {
                        Text(variableLabels[variableIndex])
                    }
                    DropdownMenu(
                        expanded = variableMenuExpanded,
                        onDismissRequest = { variableMenuExpanded = false }
                    ) {
                        variableLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onVariableIndexChange(index)
                                    variableMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Box {
                    Button(onClick = { operationMenuExpanded = true }) {
                        Text(operations[operationIndex])
                    }
                    DropdownMenu(
                        expanded = operationMenuExpanded,
                        onDismissRequest = { operationMenuExpanded = false }
                    ) {
                        operations.forEachIndexed { index, operation ->
                            DropdownMenuItem(
                                text = { Text(operation) },
                                onClick = {
                                    onOperationIndexChange(index)
                                    operationMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = operand,
                    onValueChange = { value ->
                        if (value.matches(Regex("-?\\d*(\\.\\d*)?"))) {
                            onOperandChange(value)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.operation_number_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { noteFocusRequester.requestFocus() },
                        onDone = { noteFocusRequester.requestFocus() }
                    )
                )
                IconButton(onClick = { onOperandChange("") }) {
                    Text("[x]")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(noteFocusRequester)
                        .bringIntoViewRequester(noteBringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                operationScope.launch {
                                    delay(300)
                                    noteBringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    placeholder = { Text(stringResource(R.string.operation_note_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                IconButton(onClick = { onNoteChange("") }) {
                    Text("[x]")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resultText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = note,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            },
                            sendLabel
                        )
                    )
                }) {
                    Text(stringResource(R.string.operation_send))
                }
                Button(onClick = {
                    val resolver = context.contentResolver
                    val timestamp = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        .format(LocalDateTime.now())
                    val metadata = android.content.ContentValues().apply {
                        put(MediaStore.Files.FileColumns.DISPLAY_NAME, "equiv-ratio-operation-$timestamp.txt")
                        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Files.FileColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_DOCUMENTS + "/EquivRatioSolverPlus"
                            )
                            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                        }
                    }
                    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), metadata)
                    val saved = uri?.let {
                        resolver.openOutputStream(it)?.use { output ->
                            output.write(report.toByteArray())
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            metadata.clear()
                            metadata.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                            resolver.update(it, metadata, null, null)
                        }
                        true
                    } == true
                    Toast.makeText(
                        context,
                        if (saved) savedMessage else saveFailedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(saveLabel)
                }
            }
        }
    }
}

private fun buildOperationReport(
    values: List<String>,
    yComments: String,
    xComments: String,
    variableIndex: Int,
    operation: String,
    operand: String,
    result: String,
    note: String,
    title: String
): String {
    val labels = listOf("y₁", "x₁", "y₂", "x₂")
    return buildString {
        appendLine(title)
        values.forEachIndexed { index, value ->
            val variableNote = if (index == 0 || index == 2) yComments else xComments
            append("${labels[index]} = $value")
            if (variableNote.isNotBlank()) {
                append("    ")
                append(variableNote)
            }
            appendLine()
        }
        append(labels[variableIndex])
        append(" = ")
        append(values[variableIndex])
        append(" ")
        append(operation)
        append(" ")
        append(if (operand.isBlank()) "?" else operand)
        append(" = ")
        append(result)
        if (note.isNotBlank()) {
            append("    ")
            append(note)
        }
    }
}

private fun calculateOperationResult(
    values: List<String>,
    variableIndex: Int,
    operationIndex: Int,
    operand: String
): Double? {
    val selectedValue = values.getOrNull(variableIndex)?.toDoubleOrNull()
        ?.takeIf(Double::isFinite) ?: return null
    val operandValue = operand.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
    return when (operationIndex) {
        0 -> selectedValue + operandValue
        1 -> selectedValue - operandValue
        2 -> selectedValue * operandValue
        3 -> if (operandValue == 0.0) null else selectedValue / operandValue
        else -> null
    }?.takeIf(Double::isFinite)
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
private fun ResultScreen(
    modifier: Modifier,
    inverse: Boolean,
    values: List<String>,
    yComments: String,
    xComments: String,
    onYCommentsChange: (String) -> Unit,
    onXCommentsChange: (String) -> Unit,
    decimalPlaces: Int,
    angleUnit: String
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val directTitle = stringResource(R.string.direct_proportionality)
    val inverseTitle = stringResource(R.string.inverse_proportionality)
    val saveSucceededMessage = stringResource(R.string.results_saved)
    val saveFailedMessage = stringResource(R.string.results_save_failed)
    val noteScrollState = rememberScrollState()
    val noteScope = rememberCoroutineScope()
    val noteFocusRequesters = remember { List(2) { FocusRequester() } }
    val noteBringIntoViewRequesters = remember { List(2) { BringIntoViewRequester() } }
    val format = "%.${decimalPlaces.coerceAtMost(3)}f"
    val numbers = values.map { it.toDoubleOrNull() }
    val constant = if (numbers[0] != null && numbers[1] != null && numbers[1] != 0.0) {
        if (inverse) numbers[0]!! * numbers[1]!! else numbers[0]!! / numbers[1]!!
    } else {
        null
    }
    val report = remember(inverse, values, yComments, xComments, decimalPlaces, angleUnit) {
        buildReport(
            inverse,
            values,
            yComments,
            xComments,
            decimalPlaces,
            angleUnit,
            directTitle,
            inverseTitle
        )
    }
    val exportReport = buildReport(
        inverse,
        values,
        yComments,
        xComments,
        null,
        angleUnit,
        directTitle,
        inverseTitle
    )
    val calculatedProperties = report.lines().drop(6).joinToString("\n")
    val displayedValues = values.map { formatVariableValue(it, decimalPlaces) }
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(noteScrollState)
    ) {
        Text(
            text = if (inverse) inverseTitle else directTitle,
            style = MaterialTheme.typography.titleLarge
        )
        if (constant == null) {
            Text(stringResource(R.string.result_enter_values))
        } else {
            Text(if (inverse) "y₁ · x₁ = y₂ · x₂" else "y₁ : x₁ = y₂ : x₂")
            Spacer(Modifier.padding(4.dp))
            listOf(
                "y₁=${displayedValues[0]}" to yComments,
                "x₁=${displayedValues[1]}" to xComments,
                "y₂=${displayedValues[2]}" to yComments,
                "x₂=${displayedValues[3]}" to xComments
            ).forEach { (value, comment) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(value, modifier = Modifier.weight(1f))
                    Text(comment, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.padding(4.dp))
            Text(calculatedProperties)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportReport)
                            },
                            "Send proportionality"
                        )
                    )
                }) { Text(stringResource(R.string.results_send)) }
                Button(onClick = {
                    val resolver = context.contentResolver
                    val timestamp = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        .format(LocalDateTime.now())
                    val metadata = android.content.ContentValues().apply {
                        put(MediaStore.Files.FileColumns.DISPLAY_NAME, "equiv-ratio-result-$timestamp.txt")
                        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Files.FileColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_DOCUMENTS + "/EquivRatioSolverPlus"
                            )
                            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                        }
                    }
                    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), metadata)
                    val saved = uri?.let {
                        resolver.openOutputStream(it)?.use { output ->
                            output.write(exportReport.toByteArray())
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            metadata.clear()
                            metadata.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                            resolver.update(it, metadata, null, null)
                        }
                        true
                    } == true
                    Toast.makeText(
                        context,
                        if (saved) saveSucceededMessage else saveFailedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }) { Text(stringResource(R.string.results_save)) }
            }
            Spacer(Modifier.padding(8.dp))
            Text(
                text = stringResource(R.string.result_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
            NoteInput(
                hint = stringResource(R.string.result_comment_y),
                value = yComments,
                onValueChange = onYCommentsChange,
                focusRequester = noteFocusRequesters[0],
                bringIntoViewRequester = noteBringIntoViewRequesters[0],
                onFocus = {
                    noteScope.launch {
                        delay(300)
                        noteBringIntoViewRequesters[0].bringIntoView()
                    }
                },
                onDone = {
                    if (yComments.isNotBlank() && xComments.isNotBlank()) {
                        keyboardController?.hide()
                    } else {
                        noteFocusRequesters[1].requestFocus()
                    }
                }
            )
            Spacer(Modifier.padding(2.dp))
            NoteInput(
                hint = stringResource(R.string.result_comment_x),
                value = xComments,
                onValueChange = onXCommentsChange,
                focusRequester = noteFocusRequesters[1],
                bringIntoViewRequester = noteBringIntoViewRequesters[1],
                onFocus = {
                    noteScope.launch {
                        delay(300)
                        noteBringIntoViewRequesters[1].bringIntoView()
                    }
                },
                onDone = {
                    if (yComments.isNotBlank() && xComments.isNotBlank()) {
                        keyboardController?.hide()
                    } else {
                        noteFocusRequesters[0].requestFocus()
                    }
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NoteInput(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    onFocus: () -> Unit,
    onDone: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onFocus()
                    }
                },
            singleLine = true,
            placeholder = { Text(hint) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onNext = { onDone() }
            )
        )
        IconButton(onClick = { onValueChange("") }) { Text("⌧") }
    }
}

private fun buildReport(
    inverse: Boolean,
    values: List<String>,
    yComments: String,
    xComments: String,
    decimalPlaces: Int?,
    angleUnit: String,
    directTitle: String,
    inverseTitle: String
): String {
    val y1 = values[0].toDoubleOrNull()
    val x1 = values[1].toDoubleOrNull()
    val constant = if (y1 != null && x1 != null && x1 != 0.0) {
        if (inverse) y1 * x1 else y1 / x1
    } else null
    fun variableLine(label: String, value: String, comment: String): String =
        buildString {
            append("$label=${formatVariableValue(value, decimalPlaces)}")
            if (comment.isNotBlank()) {
                append("    ")
                append(comment)
            }
        }

    val lines = mutableListOf(
        if (inverse) inverseTitle else directTitle,
        if (inverse) "y₁ · x₁ = y₂ · x₂" else "y₁ : x₁ = y₂ : x₂",
        variableLine("y₁", values[0], yComments),
        variableLine("x₁", values[1], xComments),
        variableLine("y₂", values[2], yComments),
        variableLine("x₂", values[3], xComments)
    )
    if (constant != null) {
        if (inverse) {
            val a = kotlin.math.sqrt(2.0 * kotlin.math.abs(constant))
            val vertex = kotlin.math.sqrt(kotlin.math.abs(constant))
            val sign = if (constant > 0) 1 else -1
            lines += listOf(
                "A=${formatNumber(constant, decimalPlaces)}",
                "a=${formatNumber(a, decimalPlaces)}",
                "V1=(${formatNumber(sign * vertex, decimalPlaces)}, ${formatNumber(vertex, decimalPlaces)})",
                "V2=(${formatNumber(-sign * vertex, decimalPlaces)}, ${formatNumber(-vertex, decimalPlaces)})",
                "c=${formatNumber(2 * kotlin.math.sqrt(kotlin.math.abs(constant)), decimalPlaces)}",
                "e=${formatNumber(kotlin.math.sqrt(2.0), decimalPlaces)}"
            )
        } else {
            val angle = Math.toDegrees(kotlin.math.atan(constant))
            lines += "m=${formatNumber(constant, decimalPlaces)}"
            lines += formatAngle(angle, angleUnit, decimalPlaces)
        }
    }
    return lines.joinToString("\n")
}

private fun formatVariableValue(value: String, decimalPlaces: Int?): String {
    if (decimalPlaces == null) return value
    val number = value.toDoubleOrNull() ?: return value
    return String.format(
        Locale.US,
        "%.${decimalPlaces.coerceIn(0, 9)}f",
        number
    )
}

private fun formatNumber(value: Double, decimalPlaces: Int?): String {
    return decimalPlaces?.let {
        String.format(Locale.US, "%.${it.coerceAtMost(3)}f", value)
    } ?: value.toString()
}

private fun formatAngle(angleDegrees: Double, angleUnit: String, decimalPlaces: Int?): String {
    val format = decimalPlaces?.let { "%.${it.coerceAtMost(3)}f" }
    val unit = AngleUnit.entries.firstOrNull { it.name == angleUnit } ?: AngleUnit.DEGREES
    return when (unit) {
        AngleUnit.DEGREES -> String.format(Locale.US, "α=%.0f°", angleDegrees)
        AngleUnit.DEGREES_MINUTES -> {
            val degrees = kotlin.math.floor(kotlin.math.abs(angleDegrees))
            val minutes = ((kotlin.math.abs(angleDegrees) - degrees) * 60).toInt()
            "α=${if (angleDegrees < 0) "-" else ""}${degrees.toInt()}°${minutes}′"
        }
        AngleUnit.DEGREES_MINUTES_SECONDS -> {
            val absolute = kotlin.math.abs(angleDegrees)
            val degrees = kotlin.math.floor(absolute)
            val minutesValue = (absolute - degrees) * 60
            val minutes = kotlin.math.floor(minutesValue)
            val seconds = ((minutesValue - minutes) * 60).toInt()
            "α=${if (angleDegrees < 0) "-" else ""}${degrees.toInt()}°${minutes.toInt()}′${seconds}″"
        }
        AngleUnit.RADIANS -> if (format != null) {
            String.format(Locale.US, "α=$format rad", Math.toRadians(angleDegrees))
        } else {
            "α=${Math.toRadians(angleDegrees)} rad"
        }
    }
}

@Composable
private fun GraphScreen(
    modifier: Modifier,
    inverse: Boolean,
    values: List<String>,
    decimalPlaces: Int,
    angleUnit: String
) {
    val context = LocalContext.current
    val graphSavedMessage = stringResource(R.string.graph_saved)
    val graphSaveFailedMessage = stringResource(R.string.graph_save_failed)
    val graphNotReadyMessage = stringResource(R.string.graph_not_ready)
    val view = LocalView.current
    var canvasBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val numbers = values.map { it.toDoubleOrNull() }
    val y1 = numbers[0]
    val x1 = numbers[1]
    val y2 = numbers[2]
    val x2 = numbers[3]
    val constant = if (y1 != null && x1 != null && y1 != 0.0 && x1 != 0.0) {
        if (inverse) y1 * x1 else y1 / x1
    } else if (y2 != null && x2 != null && y2 != 0.0 && x2 != 0.0) {
        if (inverse) y2 * x2 else y2 / x2
    } else {
        null
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val axisColor = MaterialTheme.colorScheme.onSurface
        val graphColor = MaterialTheme.colorScheme.primary
        val vertexColor = MaterialTheme.colorScheme.tertiary
        Text(
            text = stringResource(
                if (inverse) R.string.inverse_proportionality else R.string.direct_proportionality
            ),
            style = MaterialTheme.typography.titleLarge
        )
        if (constant == null) {
            Text(text = stringResource(R.string.graph_enter_values))
        } else {
            val numberFormat = "%.${decimalPlaces.coerceAtMost(3)}f"
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 240.dp)
                    .onGloballyPositioned { canvasBounds = it.boundsInRoot() }
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val coordinateExtent = if (inverse) {
                    maxOf(
                        4f,
                        2f * kotlin.math.sqrt(kotlin.math.abs(constant.toFloat()))
                    )
                } else {
                    6f
                }
                val scale = minOf(size.width, size.height) / (2f * coordinateExtent)
                drawLine(axisColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 2f)
                drawLine(axisColor, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 2f)
                if (inverse) {
                    val path = Path()
                    var started = false
                    for (pixelX in 0 until size.width.toInt()) {
                        val x = (pixelX - centerX) / scale
                        if (kotlin.math.abs(x) < 1f / scale) {
                            started = false
                            continue
                        }
                        val y = (constant / x).toFloat()
                        val pixelY = centerY - y * scale
                        if (pixelY.isFinite() && pixelY in 0f..size.height) {
                            if (!started) {
                                path.moveTo(pixelX.toFloat(), pixelY)
                                started = true
                            } else {
                                path.lineTo(pixelX.toFloat(), pixelY)
                            }
                        } else {
                            started = false
                        }
                    }
                    drawPath(path, graphColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    val projectionEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(10f, 8f),
                        phase = 0f
                    )
                    val vertexCoordinate = kotlin.math.sqrt(kotlin.math.abs(constant))
                    val vertexSign = if (constant > 0.0) 1.0 else -1.0
                    val vertices = listOf(
                        "V1" to (vertexSign * vertexCoordinate to vertexCoordinate),
                        "V2" to (-vertexSign * vertexCoordinate to -vertexCoordinate)
                    )
                    val vertexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = vertexColor.toArgb()
                        textSize = 14f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    vertices.forEach { (label, coordinates) ->
                        val vertexX = centerX + coordinates.first.toFloat() * scale
                        val vertexY = centerY - coordinates.second.toFloat() * scale
                        drawLine(
                            color = vertexColor,
                            start = Offset(vertexX, vertexY),
                            end = Offset(vertexX, centerY),
                            strokeWidth = 3f,
                            pathEffect = projectionEffect
                        )
                        drawLine(
                            color = vertexColor,
                            start = Offset(vertexX, vertexY),
                            end = Offset(centerX, vertexY),
                            strokeWidth = 3f,
                            pathEffect = projectionEffect
                        )
                        drawCircle(
                            color = vertexColor,
                            radius = 8f,
                            center = Offset(vertexX, vertexY)
                        )
                        val labelX = if (constant > 0.0) {
                            vertexX + 10f
                        } else {
                            vertexX - vertexPaint.measureText(label) - 10f
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            labelX,
                            vertexY - 10f,
                            vertexPaint
                        )
                    }
                } else {
                    val yAtLeft = centerY + constant.toFloat() * centerX
                    val yAtRight = centerY - constant.toFloat() * (size.width - centerX)
                    drawLine(
                        graphColor,
                        Offset(0f, yAtLeft),
                        Offset(size.width, yAtRight),
                        strokeWidth = 4f
                    )
                    val angleDegrees = Math.toDegrees(kotlin.math.atan(constant))
                    val arcRadius = minOf(size.width, size.height) * 0.22f
                    drawArc(
                        color = graphColor,
                        startAngle = 0f,
                        sweepAngle = -angleDegrees.toFloat(),
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = androidx.compose.ui.geometry.Size(
                            arcRadius * 2f,
                            arcRadius * 2f
                        ),
                        style = Stroke(width = 5f)
                    )

                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = graphColor.toArgb()
                        textSize = 16f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val labelX = centerX + arcRadius * 0.7f
                    val labelY = if (constant < 0.0) {
                        centerY - arcRadius * 0.55f
                    } else {
                        centerY + arcRadius * 0.55f
                    }
                    val unit = AngleUnit.entries.firstOrNull { it.name == angleUnit } ?: AngleUnit.DEGREES
                    val angleValue = when (unit) {
                        AngleUnit.DEGREES -> String.format(
                            Locale.US,
                            "α = %.0f°",
                            angleDegrees
                        )
                        AngleUnit.DEGREES_MINUTES -> {
                            val degrees = kotlin.math.floor(kotlin.math.abs(angleDegrees))
                            val minutes = (kotlin.math.abs(angleDegrees) - degrees) * 60.0
                            String.format(
                                Locale.US,
                                "α = %s%d°%d′",
                                if (angleDegrees < 0) "-" else "",
                                degrees.toInt(),
                                minutes.toInt()
                            )
                        }
                        AngleUnit.DEGREES_MINUTES_SECONDS -> {
                            val absoluteAngle = kotlin.math.abs(angleDegrees)
                            val degrees = kotlin.math.floor(absoluteAngle)
                            val minutesValue = (absoluteAngle - degrees) * 60.0
                            val minutes = kotlin.math.floor(minutesValue)
                            val seconds = (minutesValue - minutes) * 60.0
                            String.format(
                                Locale.US,
                                "α = %s%d°%d′%d″",
                                if (angleDegrees < 0) "-" else "",
                                degrees.toInt(),
                                minutes.toInt(),
                                seconds.toInt()
                            )
                        }
                        AngleUnit.RADIANS -> String.format(
                            Locale.US,
                            "α = $numberFormat rad",
                            Math.toRadians(angleDegrees)
                        )
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format(Locale.US, "m = $numberFormat", constant),
                        labelX,
                        labelY,
                        labelPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        angleValue,
                        labelX,
                        labelY + labelPaint.textSize * 1.3f,
                        labelPaint
                    )
                }
                if (inverse) {
                    val absoluteConstant = kotlin.math.abs(constant)
                    val semiAxis = kotlin.math.sqrt(2.0 * absoluteConstant)
                    val vertexCoordinate = kotlin.math.sqrt(absoluteConstant)
                    val vertexSign = if (constant > 0.0) 1.0 else -1.0
                    val vertex1 = vertexSign * vertexCoordinate
                    val vertex2 = -vertexSign * vertexCoordinate
                    val linearEccentricity = 2.0 * kotlin.math.sqrt(absoluteConstant)
                    val propertyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = graphColor.toArgb()
                        textSize = 13f * density
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val propertyLines = listOf(
                        String.format(Locale.US, "A=$numberFormat", constant),
                        String.format(Locale.US, "a=$numberFormat", semiAxis),
                        String.format(
                            Locale.US,
                            "V1($numberFormat, $numberFormat)",
                            vertex1,
                            vertexCoordinate
                        ),
                        String.format(
                            Locale.US,
                            "V2($numberFormat, $numberFormat)",
                            vertex2,
                            -vertexCoordinate
                        ),
                        String.format(Locale.US, "c=$numberFormat", linearEccentricity),
                        String.format(Locale.US, "e=$numberFormat", kotlin.math.sqrt(2.0))
                    )
                    val propertyX = centerX + coordinateExtent * scale * 0.18f
                    val lineHeight = propertyPaint.textSize * 1.25f
                    val propertyHeight = lineHeight * propertyLines.size
                    val propertyY = if (constant > 0.0) {
                        centerY + coordinateExtent * scale * 0.78f
                    } else {
                        maxOf(propertyHeight, centerY - coordinateExtent * scale * 0.78f)
                    }
                    propertyLines.forEachIndexed { index, line ->
                        drawContext.canvas.nativeCanvas.drawText(
                            line,
                            propertyX,
                            if (constant > 0.0) {
                                propertyY - (propertyLines.lastIndex - index) * lineHeight
                            } else {
                                propertyY - (propertyLines.lastIndex - index) * lineHeight
                            },
                            propertyPaint
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val bounds = canvasBounds
                    if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                        Toast.makeText(context, graphNotReadyMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        val left = bounds.left.toInt().coerceAtLeast(0)
                        val top = bounds.top.toInt().coerceAtLeast(0)
                        val right = bounds.right.toInt().coerceAtMost(view.width)
                        val bottom = bounds.bottom.toInt().coerceAtMost(view.height)
                        val bitmap = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888)
                        val canvas = AndroidCanvas(bitmap)
                        canvas.translate(-left.toFloat(), -top.toFloat())
                        view.draw(canvas)
                        val resolver = context.contentResolver
                        val timestamp = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                            .format(LocalDateTime.now())
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "equiv-ratio-graph-$timestamp.png")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EquivRatioSolverPlus")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (uri == null) {
                            Toast.makeText(context, graphSaveFailedMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            val saved = resolver.openOutputStream(uri)?.use { output ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                            } == true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                values.clear()
                                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                resolver.update(uri, values, null, null)
                            }
                            Toast.makeText(
                                context,
                                if (saved) graphSavedMessage else graphSaveFailedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.save_graph_to_gallery))
            }
        }
    }
}

@Composable
private fun LegalText(
    text: String,
    headings: List<String>,
    onEmailClick: (() -> Unit)? = null,
    onLicenseLinkClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val headingStyle = SpanStyle(fontWeight = FontWeight.Bold)
    val annotatedText = buildAnnotatedString {
        text.lines().forEachIndexed { index, line ->
            if (line in headings) {
                withStyle(headingStyle) {
                    append(line)
                }
            } else {
                append(line)
            }
            val email = "scaynialp@gmail.com"
            val emailStart = line.indexOf(email)
            if (onEmailClick != null && emailStart >= 0) {
                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    start = length - line.length + emailStart,
                    end = length - line.length + emailStart + email.length
                )
                addStringAnnotation(
                    tag = "email",
                    annotation = "mailto:$email",
                    start = length - line.length + emailStart,
                    end = length - line.length + emailStart + email.length
                )
            }
            val licenseUrl = "https://www.gnu.org/licenses/"
            val licenseUrlStart = line.indexOf(licenseUrl)
            if (onLicenseLinkClick != null && licenseUrlStart >= 0) {
                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    start = length - line.length + licenseUrlStart,
                    end = length - line.length + licenseUrlStart + licenseUrl.length
                )
                addStringAnnotation(
                    tag = "license",
                    annotation = licenseUrl,
                    start = length - line.length + licenseUrlStart,
                    end = length - line.length + licenseUrlStart + licenseUrl.length
                )
            }
            if (index < text.lines().lastIndex) {
                append('\n')
            }
        }
    }
    if (onEmailClick == null && onLicenseLinkClick == null) {
        Text(
            text = annotatedText,
            modifier = modifier,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        ClickableText(
            text = annotatedText,
            modifier = modifier,
            style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
            onClick = { offset ->
                if (annotatedText.getStringAnnotations("email", offset, offset).isNotEmpty()) {
                    onEmailClick?.invoke()
                }
                if (annotatedText.getStringAnnotations("license", offset, offset).isNotEmpty()) {
                    onLicenseLinkClick?.invoke()
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun InputRow(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    hint: String,
    variableLabel: String,
    focusRequester: FocusRequester,
    bringIntoViewRequester: BringIntoViewRequester,
    onFocus: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("-?\\d*\\.?\\d*"))) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocus()
                    }
                },
            readOnly = readOnly,
            singleLine = true,
            placeholder = { Text(hint) },
            label = if (value.isNotEmpty()) {
                { Text(" $variableLabel ") }
            } else {
                null
            },
            textStyle = TextStyle(
                color = if (readOnly) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (readOnly) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                unfocusedContainerColor = if (readOnly) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                }
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onDone() }
            )
        )
        IconButton(onClick = onDelete) {
            Text("⌧")
        }
    }
}

private fun calculateMissingValue(
    values: List<String>,
    missingIndex: Int,
    inverse: Boolean
): String? {
    val numbers = values.map { it.toDoubleOrNull() }
    if (numbers.withIndex().any { (index, value) ->
            index != missingIndex && (value == null || value == 0.0)
        }
    ) return null
    val y1 = numbers[0] ?: 0.0
    val x1 = numbers[1] ?: 0.0
    val y2 = numbers[2] ?: 0.0
    val x2 = numbers[3] ?: 0.0
    val result = if (inverse) {
        when (missingIndex) {
            0 -> y2 * x2 / x1
            1 -> y1 * x2 / y2
            2 -> y1 * x1 / x2
            else -> y1 * x1 / y2
        }
    } else {
        when (missingIndex) {
            0 -> y2 * x1 / x2
            1 -> y1 * x2 / y2
            2 -> y1 * x2 / x1
            else -> y2 * x1 / y1
        }
    }
    return result.toString()
}

@Composable
private fun MenuItem(
    labelResource: Int,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(labelResource)) },
        onClick = onClick
    )
}
