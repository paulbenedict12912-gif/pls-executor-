package com.kuroko.apkforge

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlParser {

    private const val RES_STRING_POOL = 0x0001
    private const val RES_XML_START_ELEMENT = 0x0102
    private const val RES_XML_END_ELEMENT = 0x0103
    private const val RES_XML_START_NAMESPACE = 0x0100

    fun decode(data: ByteArray): String {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.short.toInt() and 0xFFFF
        if (magic != 0x0003) return "not axml (magic=0x${magic.toString(16)})"

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")

        buf.position(8)

        var stringPool: List<String>? = null

        while (buf.remaining() >= 8) {
            val chunkStart = buf.position()
            val type = buf.short.toInt() and 0xFFFF
            val headerSize = buf.short.toInt() and 0xFFFF
            val chunkSize = buf.int
            if (chunkSize <= 0 || chunkStart + chunkSize > data.size) break

            when (type) {
                RES_STRING_POOL -> {
                    buf.position(chunkStart + 8)
                    stringPool = parseStringPool(buf)
                }
                RES_XML_START_NAMESPACE -> { }
                RES_XML_START_ELEMENT -> {
                    val pool = stringPool ?: emptyList()
                    buf.position(chunkStart + headerSize)
                    buf.int
                    buf.int
                    buf.int
                    val nameIdx = buf.int
                    buf.short
                    val attrSize = buf.short.toInt() and 0xFFFF
                    val attrCount = buf.short.toInt() and 0xFFFF
                    buf.short
                    buf.short
                    buf.short

                    val name = pool.getOrElse(nameIdx) { "?" }
                    sb.append("<").append(name)

                    var attrPos = chunkStart + headerSize + 20
                    repeat(attrCount) {
                        buf.position(attrPos)
                        buf.int
                        val aName = buf.int
                        buf.int
                        buf.int
                        buf.position(attrPos + 15)
                        val dataType = buf.get().toInt() and 0xFF
                        val dataValue = buf.int

                        val attrName = pool.getOrElse(aName) { "?" }
                        val value = when (dataType) {
                            0x03 -> pool.getOrElse(dataValue) { "" }
                            0x10 -> dataValue.toString()
                            0x11 -> "0x${dataValue.toString(16)}"
                            0x12 -> if (dataValue == 0) "false" else "true"
                            0x01 -> "@0x${dataValue.toString(16)}"
                            0x02 -> "?0x${dataValue.toString(16)}"
                            0x04 -> dataValue.toFloat().toString()
                            else -> "0x${dataValue.toString(16)}"
                        }
                        sb.append(" ").append(attrName).append("=\"").append(value).append("\"")
                        attrPos += attrSize
                    }
                    sb.append(">")
                }
                RES_XML_END_ELEMENT -> {
                    val pool = stringPool ?: emptyList()
                    buf.position(chunkStart + headerSize + 8)
                    val nameIdx = buf.int
                    val name = pool.getOrElse(nameIdx) { "?" }
                    sb.append("</").append(name).append(">")
                }
            }

            buf.position(chunkStart + chunkSize)
        }
        return sb.toString()
    }

    private fun parseStringPool(buf: ByteBuffer): List<String> {
        val stringCount = buf.int
        buf.int
        val flags = buf.int
        val stringsStart = buf.int
        buf.int
        val isUtf8 = (flags and (1 shl 8)) != 0

        val offsetsStart = buf.position()
        val offsets = IntArray(stringCount) { buf.getInt(offsetsStart + it * 4) }
        val basePos = offsetsStart - 8 + stringsStart

        val out = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val p = basePos + offsets[i]
            buf.position(p)
            out.add(if (isUtf8) readUtf8(buf) else readUtf16(buf))
        }
        return out
    }

    private fun readUtf8(buf: ByteBuffer): String {
        var len = buf.get().toInt() and 0xFF
        if (len and 0x80 != 0) len = ((len and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        val skip = buf.get().toInt() and 0xFF
        if (skip and 0x80 != 0) buf.get()
        val bytes = ByteArray(len)
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16(buf: ByteBuffer): String {
        var len = buf.short.toInt() and 0xFFFF
        if (len and 0x8000 != 0) len = ((len and 0x7FFF) shl 16) or (buf.short.toInt() and 0xFFFF)
        val chars = CharArray(len)
        for (i in 0 until len) chars[i] = buf.short.toInt().toChar()
        return String(chars)
    }
}
