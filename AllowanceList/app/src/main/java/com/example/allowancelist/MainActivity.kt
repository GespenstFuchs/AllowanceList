package com.example.allowancelist

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allowancelist.ui.theme.AllowanceListTheme
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

/**
 * MainActivity
 */
class MainActivity : ComponentActivity() {
    /**
     * 生成処理
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // エッジツーエッジの表示を有効にして、システムUI（ステータスバーなど）に重なって表示可能にする。
        enableEdgeToEdge()

        // ComposeのUIコンテンツを設定する。
        setContent {
            // カスタムテーマを適用する。
            AllowanceListTheme {
                // Scaffoldは画面の基本レイアウト構造を提供するコンポーザブル
                // Modifier.fillMaxSize()で画面全体を占有するように指定する。
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Scaffoldから提供される内側のパディング(innerPadding)を適用してAllowancelistコンポーザブルを表示する。
                    Allowancelist(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Composable関数：Allowancelist
 * （@Composableアノテーションにより、この関数はCompose UIとして利用可能にする。）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Allowancelist(modifier: Modifier = Modifier) {
    // 背景色を定義する。
    val defaultColor = Color(0xFFE0FFFF)
    val selectedColor = Color(0xFFFFEAEA)

    // 項目データクラス
    data class ItemData(
        val checked: MutableState<Boolean> = mutableStateOf(false),
        var raw: MutableState<String> = mutableStateOf("初期値"),    // カンマ区切り文字列
        var bgColor: MutableState<Color> = mutableStateOf(defaultColor)
    )

    val context = LocalContext.current

    // プリファレンスを読み込み、オブジェクトを生成する。
    val sharedPref = context.getSharedPreferences("allowance_list", Context.MODE_PRIVATE)

    // 保存しているテキストを読み込む。
    val readText = sharedPref.getString("ALLOWANCE_LIST_TEXT", "")

    // 改行ごとに分割して、空行は除外する。
    val fileItems: List<String> = readText
        ?.lines()
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    // リストにデータを設定する。
    val itemsList = remember {
        mutableStateListOf<ItemData>().apply {
            addAll(fileItems.map { raw ->
                ItemData(raw = mutableStateOf(raw))
            })
        }
    }

    // テキスト保存処理
    fun saveToText() {
        sharedPref.edit(commit = true) {
            putString(
                "ALLOWANCE_LIST_TEXT",
                itemsList.joinToString("\n") { it.raw.value })
        }
    }

    // 各フォーマットを定義する。
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val moneyFormatter = DecimalFormat("#,###")

    // 項目対応変数
    // 合計金額
    var totalText by remember { mutableStateOf("合計: ¥0") }
    // 日付
    var dateText by remember { mutableStateOf(LocalDate.now().format(dateFormatter)) }
    // 金額
    var yenText by remember { mutableStateOf("") }
    // メモ
    var memoText by remember { mutableStateOf("") }
    // 合計金額文字色
    var totalTextColor by remember { mutableStateOf(Color.Black) }
    // 削除ボタン活性状態
    var deleteButtonEnabled by remember { mutableStateOf(false) }
    // 保存ボタン活性状態
    var saveButtonEnabled by remember { mutableStateOf(false) }

    // 現在選択中の項目位置（初期値：-1（選択無し））
    val selectedItemIndex = remember { mutableIntStateOf(-1) }

    // チェック編集処理
    fun checkEdit(): Boolean {
        if (dateText.trim().isBlank()) {
            AlertDialog.Builder(context)
                .setTitle("エラー")
                .setMessage("日付が未入力")
                .setPositiveButton("OK", null)
                .show()
            return false
        }

        if (dateText.trim().length > 10) {
            AlertDialog.Builder(context)
                .setTitle("エラー")
                .setMessage("日付は１０桁まで入力可能")
                .setPositiveButton("OK", null)
                .show()
            return false
        }

        if (!dateText.trim().matches(Regex("[/0-9]+"))) {
            AlertDialog.Builder(context)
                .setTitle("エラー")
                .setMessage("日付は半角数値・/のみ入力可能")
                .setPositiveButton("OK", null)
                .show()
            return false
        }

        val checkValue = yenText.trim()

        if (checkValue.trim().isBlank()) {
            AlertDialog.Builder(context)
                .setTitle("エラー")
                .setMessage("金額が未入力")
                .setPositiveButton("OK", null)
                .show()
            return false
        }

        // 【-】の位置を判定する。
        val index = checkValue.indexOf("-")
        if (index != -1) {
            // ハイフンが存在する場合
            if (index != 0 || checkValue.count { it == '-' } != 1) {
                // ハイフンの位置・個数が不正の場合
                AlertDialog.Builder(context)
                    .setTitle("エラー")
                    .setMessage("-の位置が不正")
                    .setPositiveButton("OK", null)
                    .show()
                return false
            }
        }

        // 半角数値チェック
        if (checkValue.all { it in '0'..'9' || it == '-' }) {
            // 半角数値のみ
            dateText = dateText.trim()
            yenText = checkValue
        } else {
            AlertDialog.Builder(context)
                .setTitle("エラー")
                .setMessage("金額に数値以外が入力")
                .setPositiveButton("OK", null)
                .show()
            return false
        }

        return true
    }

    // 合計金額更新処理
    fun updateTotal() {
        var total = 0
        itemsList.forEach { it ->
            val parts = it.raw.value.split(",", limit = 3)
            // この処理が行われる場合、１桁目に-が存在する前提
            if (parts[1].all { it in '0'..'9' || it == '-' }) {
                total += parts[1].toInt()
            }
        }

        // ハイフンの有無を判定する。
        totalTextColor = if (total.toString().indexOf("-") == 0) {
            Color.Red
        } else {
            Color.Black
        }

        totalText = "合計: ¥" + moneyFormatter.format(total)
    }

    // 初期表示時に合計金額を更新する。
    updateTotal()

    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFE6E6FA))
            .padding(16.dp)
    ) {
        // 合計金額ラベル
        Text(
            text = totalText,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                color = totalTextColor
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 枠線
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 4.dp,
                    color = Color(0xFF8A2BE2),
                )
                .background(Color(0xFF8A2BE2), shape = RoundedCornerShape(8.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF8A2BE2))
            ) {
                // カンマで分割する。
                itemsIndexed(itemsList) { index, item ->
                    val itemParts = item.raw.value.split(",", limit = 3)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(item.bgColor.value)
                            .combinedClickable(
                                onClick = {
                                    // 全項目の背景色をデフォルトにリセットする。
                                    itemsList.forEach { it.bgColor.value = defaultColor }

                                    // 選択位置を保持する。
                                    selectedItemIndex.intValue = index

                                    // 選択した項目の背景色を変更する。
                                    item.bgColor.value = selectedColor

                                    // 値を設定する。
                                    dateText = itemParts[0]
                                    yenText = itemParts[1].replace(",", "")
                                    memoText = itemParts[2]

                                    // 保存ボタンを活性にする。
                                    saveButtonEnabled = true
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 削除用チェックボックス
                        Checkbox(
                            checked = item.checked.value,
                            onCheckedChange = { isChecked ->
                                // チェックを設定する。
                                item.checked.value = isChecked

                                // 削除ボタンの活性状態を設定する。
                                deleteButtonEnabled = itemsList.any { listItem -> listItem.checked.value }
                            }
                        )

                        Column {
                            // 設定する文字色を設定する。
                            val setColor = if (itemParts[1].startsWith("-")) Color.Red else Color.Black

                            // 日付・金額
                            Text(
                                text = " ${itemParts[0]}    ¥${moneyFormatter.format(itemParts[1].toInt())}",
                                color = setColor,
                                fontSize = 20.sp
                            )

                            // メモ
                            Text(
                                text = " ${itemParts[2]}",
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // 項目間のスペース
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // リスト下部の余白
        Spacer(modifier = Modifier.height(16.dp))

        // １行目
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 日付
            Text(
                text = "日付：",
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = dateText,
                onValueChange = { dateText = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE0FFFF),
                    unfocusedContainerColor = Color(0xFFE0FFFF),
                    disabledContainerColor = Color(0xFFE0FFFF),
                    errorContainerColor = Color(0xFFE0FFFF),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.Black,
                    errorTextColor = Color.Black,
                ),
            )
        }

        // １行目・２行目の空白
        Spacer(modifier = Modifier.height(16.dp))

        // ２行目
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "金額：",
                color = Color.Black
            )

            // 金額
            TextField(
                value = yenText,
                onValueChange = { yenText = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE0FFFF),
                    unfocusedContainerColor = Color(0xFFE0FFFF),
                    disabledContainerColor = Color(0xFFE0FFFF),
                    errorContainerColor = Color(0xFFE0FFFF),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.Black,
                    errorTextColor = Color.Black,
                ),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // －ボタン
            OutlinedButton(
                onClick = {
                    yenText = if (yenText.startsWith("-")) {
                        // 先頭が - の場合 → 削除
                        yenText.removePrefix("-")
                    } else {
                        // 先頭に - がない場合 → 追加
                        "-$yenText"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFDAB9),
                    contentColor = Color.Black,
                )
            ) {
                Text(text = "－")
            }
        }

        // ２行目・３行目の空白
        Spacer(modifier = Modifier.height(16.dp))

        // ３行目
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.imePadding()
        ) {
            // メモ
            Text(
                modifier = Modifier.imePadding(),
                text = "メモ：",
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = memoText,
                onValueChange = { memoText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFE0FFFF),
                    unfocusedContainerColor = Color(0xFFE0FFFF),
                    disabledContainerColor = Color(0xFFE0FFFF),
                    errorContainerColor = Color(0xFFE0FFFF),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    disabledTextColor = Color.Black,
                    errorTextColor = Color.Black,
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ボタンを横並びに配置する。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    if (checkEdit()) {
                        val newRaw = "${dateText},${yenText},$memoText"
                        itemsList.add(0, ItemData(raw = mutableStateOf(newRaw)))
                        saveToText()
                        updateTotal()

                        // 各入力項目をクリアする。
                        dateText = LocalDate.now().format(dateFormatter)
                        yenText = ""
                        memoText = ""

                        Toast.makeText(context, "新規追加しました。", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFDAB9),
                contentColor = Color.Black,
            )) {
                Text(text = "新規追加")
            }
            OutlinedButton(
                enabled = deleteButtonEnabled,
                onClick = {
                    // チェックが入っている項目を削除する。
                    itemsList.removeAll { it.checked.value }
                    saveToText()
                    updateTotal()

                    // 各入力項目をクリアする。
                    dateText = LocalDate.now().format(dateFormatter)
                    yenText = ""
                    memoText = ""

                    Toast.makeText(context, "削除しました。", Toast.LENGTH_SHORT).show()

                    // 削除ボタンを非活性にする。
                    deleteButtonEnabled = false
                },
                colors = if (deleteButtonEnabled) {
                    // 活性の場合
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFDAB9),
                        contentColor = Color.Black,
                    )
                } else {
                    // 非活性の場合
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFDAB9),
                        contentColor = Color.Gray,
                    )
                }
            ) {
                Text(text = "削除")
            }
            OutlinedButton(
                enabled = saveButtonEnabled,
                onClick = {
                    if (selectedItemIndex.intValue != -1 && selectedItemIndex.intValue < itemsList.size) {
                        if (checkEdit()) {
                            val item = itemsList[selectedItemIndex.intValue]
                            val newRaw = "${dateText},${yenText},$memoText"
                            item.raw.value = newRaw
                            saveToText()
                            updateTotal()
                            Toast.makeText(context, "保存しました。", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = if (saveButtonEnabled) {
                    // 活性の場合
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFDAB9),
                        contentColor = Color.Black,
                    )
                } else {
                    // 非活性の場合
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFDAB9),
                        contentColor = Color.Gray,
                    )
                }
            ) {
                Text(text = "保存")
            }
        }
    }
}

// プレビュー用のComposable関数
// Android Studioのプレビュー機能でUIを確認するために使用される。
@Preview(showBackground = true)
@Composable
fun PreviewAllowancelist() {
    // プレビュー時も同じテーマを適用して、Greetingコンポーザブルを表示
    AllowanceListTheme {
        Allowancelist()
    }
}