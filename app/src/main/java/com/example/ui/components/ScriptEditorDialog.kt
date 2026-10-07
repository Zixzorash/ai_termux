package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TermuxScript

@Composable
fun ScriptEditorDialog(
    initialScript: TermuxScript?,
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (TermuxScript) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialScript?.name ?: "") }
    var filename by remember {
        mutableStateOf(
            initialScript?.filename ?: if (initialScript == null) "my_script.sh" else ""
        )
    }
    var scriptPath by remember {
        mutableStateOf(
            initialScript?.scriptPath ?: "/data/data/com.termux/files/home/.shortcuts/"
        )
    }
    var content by remember {
        mutableStateOf(
            initialScript?.content ?: "#!/data/data/com.termux/files/usr/bin/bash\n\necho 'สวัสดีจาก Termux!'"
        )
    }
    var category by remember { mutableStateOf(initialScript?.category ?: "ทั่วไป") }
    var arguments by remember { mutableStateOf(initialScript?.arguments ?: "") }
    var runInBackground by remember { mutableStateOf(initialScript?.runInBackground ?: true) }
    var showInWidget by remember { mutableStateOf(initialScript?.showInWidget ?: true) }
    var isFavorite by remember { mutableStateOf(initialScript?.isFavorite ?: false) }

    var nameError by remember { mutableStateOf(false) }
    var filenameError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("script_editor_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialScript == null) "สร้างสคริปต์ใหม่ (.sh)" else "แก้ไขสคริปต์ Termux",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("ชื่อสคริปต์") },
                    placeholder = { Text("เช่น อัปเดตแพ็กเกจระบบ") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("กรุณากรอกชื่อสคริปต์") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_script_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filename
                OutlinedTextField(
                    value = filename,
                    onValueChange = {
                        filename = it
                        filenameError = it.isBlank()
                    },
                    label = { Text("ชื่อไฟล์ (.sh)") },
                    placeholder = { Text("update.sh") },
                    isError = filenameError,
                    supportingText = { if (filenameError) Text("กรุณากรอกชื่อไฟล์") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_script_filename"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category & Directory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("หมวดหมู่") },
                        placeholder = { Text("ระบบ / เครือข่าย") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_script_category"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = arguments,
                        onValueChange = { arguments = it },
                        label = { Text("อาร์กิวเมนต์ (ถ้ามี)") },
                        placeholder = { Text("-y --verbose") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_script_args"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Script Path
                OutlinedTextField(
                    value = scriptPath,
                    onValueChange = { scriptPath = it },
                    label = { Text("ตำแหน่งโฟลเดอร์เก็บสคริปต์") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_script_path"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick shortcuts path buttons
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = { scriptPath = "/data/data/com.termux/files/home/.shortcuts/" }
                    ) {
                        Text("~/.shortcuts/", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { scriptPath = "/data/data/com.termux/files/home/" }
                    ) {
                        Text("~/ (Home)", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bash Script Content
                Text(
                    text = "เนื้อหาสคริปต์ Bash (.sh):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("#!/data/data/com.termux/files/usr/bin/bash\n\npkg update -y") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("input_script_content"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick Templates
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            if (!content.startsWith("#!/data/data/com.termux/files/usr/bin/bash")) {
                                content = "#!/data/data/com.termux/files/usr/bin/bash\n" + content
                            }
                        }
                    ) {
                        Text("+ Shebang", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { content += "\necho \"เสร็จสิ้น\"" }
                    ) {
                        Text("+ Echo จบ", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("รันในเบื้องหลัง (Headless Background)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("ไม่เปิดหน้าต่างแอพ Termux ซ้อนขึ้นมา", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = runInBackground,
                        onCheckedChange = { runInBackground = it },
                        modifier = Modifier.testTag("switch_background")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("แสดงบนวิดเจ็ตหน้าจอหลัก (Widget)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text("สามารถแตะรันด่วนจากหน้าโฮมได้", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showInWidget,
                        onCheckedChange = { showInWidget = it },
                        modifier = Modifier.testTag("switch_widget")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_editor_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ยกเลิก")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            if (filename.isBlank()) {
                                filenameError = true
                                return@Button
                            }

                            val cleanFilename = if (filename.endsWith(".sh")) filename else "$filename.sh"

                            val scriptToSave = (initialScript ?: TermuxScript(
                                name = name.trim(),
                                filename = cleanFilename.trim(),
                                content = content
                            )).copy(
                                name = name.trim(),
                                filename = cleanFilename.trim(),
                                scriptPath = scriptPath.trim(),
                                content = content,
                                arguments = arguments.trim(),
                                category = category.trim().ifBlank { "ทั่วไป" },
                                runInBackground = runInBackground,
                                showInWidget = showInWidget,
                                isFavorite = isFavorite
                            )

                            onSave(scriptToSave)
                        },
                        modifier = Modifier.testTag("save_script_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("บันทึกสคริปต์")
                    }
                }
            }
        }
    }
}
