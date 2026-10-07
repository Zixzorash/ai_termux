package com.example.data.local

import com.example.data.model.TermuxScript

object DefaultScripts {
    fun getDefaultScripts(): List<TermuxScript> = listOf(
        TermuxScript(
            name = "อัปเดตระบบ Termux (pkg update)",
            filename = "update_system.sh",
            scriptPath = "/data/data/com.termux/files/home/.shortcuts/",
            content = """
                #!/data/data/com.termux/files/usr/bin/bash
                echo "🚀 เริ่มต้นอัปเดตระบบและแพ็กเกจ Termux..."
                pkg update -y && pkg upgrade -y
                echo "✅ อัปเดตแพ็กเกจเสร็จสมบูรณ์พร้อมใช้งาน!"
            """.trimIndent(),
            category = "บำรุงรักษา",
            isFavorite = true,
            showInWidget = true,
            iconName = "refresh"
        ),
        TermuxScript(
            name = "สถานะ Xiaomi 13 Pro (HyperOS)",
            filename = "xiaomi_sysinfo.sh",
            scriptPath = "/data/data/com.termux/files/home/.shortcuts/",
            content = """
                #!/data/data/com.termux/files/usr/bin/bash
                echo "⚡ === ตรวจสอบสถานะ Xiaomi 13 Pro === ⚡"
                echo "📱 อุปกรณ์: Xiaomi 13 Pro (Global ROM / HyperOS 3.1)"
                echo "🧠 RAM รวม: 12GB | ชิปเซ็ต: Snapdragon 8 Gen 2"
                echo ""
                echo "📊 --- ข้อมูลหน่วยความจำ (RAM) ---"
                free -m 2>/dev/null || cat /proc/meminfo | head -n 6
                echo ""
                echo "💾 --- พื้นที่จัดเก็บ (Storage) ---"
                df -h /data 2>/dev/null || df -h
                echo ""
                echo "⏱️ --- เวลาทำงานของระบบ (Uptime) ---"
                uptime
            """.trimIndent(),
            category = "ระบบ",
            isFavorite = true,
            showInWidget = true,
            iconName = "bolt"
        ),
        TermuxScript(
            name = "ทดสอบการเชื่อมต่อ Wi-Fi 7",
            filename = "wifi7_speedtest.sh",
            scriptPath = "/data/data/com.termux/files/home/.shortcuts/",
            content = """
                #!/data/data/com.termux/files/usr/bin/bash
                echo "📡 ตรวจสอบประสิทธิภาพ Wi-Fi 7 / Latency..."
                echo "--- ทดสอบ Ping ไปยัง Cloudflare (1.1.1.1) ---"
                ping -c 4 1.1.1.1
                echo ""
                echo "--- ทดสอบความหน่วง DNS Google (8.8.8.8) ---"
                ping -c 4 8.8.8.8
                echo "🌐 การเชื่อมต่อ Wi-Fi 7 เสถียรและพร้อมใช้งานเต็มประสิทธิภาพ"
            """.trimIndent(),
            category = "เครือข่าย",
            isFavorite = true,
            showInWidget = true,
            iconName = "wifi"
        ),
        TermuxScript(
            name = "ล้างแคชและไฟล์ขยะ (Clean Cache)",
            filename = "clean_termux.sh",
            scriptPath = "/data/data/com.termux/files/home/.shortcuts/",
            content = """
                #!/data/data/com.termux/files/usr/bin/bash
                echo "🧹 กำลังล้างแคชแพ็กเกจและไฟล์ชั่วคราว..."
                apt clean -y 2>/dev/null
                apt autoclean -y 2>/dev/null
                rm -rf ~/.cache/* 2>/dev/null
                rm -rf /data/data/com.termux/files/usr/tmp/* 2>/dev/null
                echo "✨ คืนพื้นที่จัดเก็บสำเร็จ เรียบร้อยแล้ว!"
            """.trimIndent(),
            category = "บำรุงรักษา",
            isFavorite = false,
            showInWidget = false,
            iconName = "terminal"
        ),
        TermuxScript(
            name = "สำรองโฟลเดอร์ Termux Home",
            filename = "backup_home.sh",
            scriptPath = "/data/data/com.termux/files/home/.shortcuts/",
            content = """
                #!/data/data/com.termux/files/usr/bin/bash
                echo "📦 กำลังสำรองข้อมูลโฟลเดอร์ ~/.shortcuts/..."
                BACKUP_DIR="${'$'}HOME/termux_backups"
                mkdir -p "${'$'}BACKUP_DIR"
                TAR_NAME="shortcuts_${'$'}(date +%Y%m%d_%H%M%S).tar.gz"
                tar -czvf "${'$'}BACKUP_DIR/${'$'}TAR_NAME" -C "${'$'}HOME" .shortcuts 2>/dev/null
                echo "🎉 บันทึกไฟล์สำรอง: ${'$'}BACKUP_DIR/${'$'}TAR_NAME"
            """.trimIndent(),
            category = "สำรองข้อมูล",
            isFavorite = false,
            showInWidget = false,
            iconName = "code"
        )
    )
}
