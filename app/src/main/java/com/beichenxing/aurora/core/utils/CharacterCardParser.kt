package com.beichenxing.aurora.core.utils

import android.content.Context
import android.net.Uri
import com.beichenxing.aurora.domain.model.Character
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Parses SillyTavern-compatible character cards.
 *
 * Supported formats:
 * - PNG V2: tEXt chunk with key "chara", value = Base64(JSON)
 * - PNG V3: tEXt chunk with key "ccv3", value = Base64(JSON)
 * - CharX: ZIP archive containing card.json
 * - JSON: raw character JSON
 */
object CharacterCardParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Parse a character card from a URI.
     * Returns null if parsing fails.
     */
    fun parse(context: Context, uri: Uri): Character? {
        return try {
            val fileName = getFileName(context, uri) ?: ""
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            when {
                fileName.endsWith(".png", ignoreCase = true) -> parsePng(inputStream)
                fileName.endsWith(".charx", ignoreCase = true) -> parseCharX(inputStream)
                fileName.endsWith(".json", ignoreCase = true) -> parseJson(inputStream)
                else -> {
                    // Try to detect format from content
                    val bytes = inputStream.readBytes()
                    when {
                        isPng(bytes) -> parsePng(bytes.inputStream())
                        isZip(bytes) -> parseCharX(bytes.inputStream())
                        else -> parseJson(bytes.inputStream())
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse character card from URI: $uri")
            null
        }
    }

    // ─── PNG Parsing ──────────────────────────────────────────────────────────

    /**
     * Extract character data from PNG tEXt chunks.
     * Supports both V2 (key="chara") and V3 (key="ccv3").
     */
    private fun parsePng(inputStream: InputStream): Character? {
        val bytes = inputStream.readBytes()
        if (!isPng(bytes)) return null

        // PNG structure: 8-byte signature + chunks
        // Each chunk: 4-byte length + 4-byte type + data + 4-byte CRC
        var offset = 8 // skip PNG signature

        while (offset < bytes.size - 12) {
            val length = readInt(bytes, offset)
            val type = String(bytes, offset + 4, 4, Charsets.ISO_8859_1)
            val dataStart = offset + 8

            if (type == "tEXt" || type == "iTXt") {
                val chunkData = bytes.copyOfRange(dataStart, dataStart + length)
                val jsonStr = extractTextChunk(chunkData, type)
                if (jsonStr != null) {
                    val character = parseCharacterJson(jsonStr)
                    if (character != null) return character
                }
            }

            // Move to next chunk: length(4) + type(4) + data(length) + CRC(4)
            offset += 12 + length
        }

        return null
    }

    /**
     * Extract and decode Base64 JSON from a PNG tEXt or iTXt chunk.
     * Returns decoded JSON string if the chunk key is "chara" or "ccv3".
     */
    private fun extractTextChunk(data: ByteArray, chunkType: String): String? {
        return try {
            if (chunkType == "tEXt") {
                // tEXt format: keyword\0text (null-separated)
                val nullIndex = data.indexOf(0.toByte())
                if (nullIndex < 0) return null

                val keyword = String(data, 0, nullIndex, Charsets.ISO_8859_1)
                if (keyword != "chara" && keyword != "ccv3") return null

                val value = String(data, nullIndex + 1, data.size - nullIndex - 1, Charsets.ISO_8859_1)
                String(Base64.getDecoder().decode(value), Charsets.UTF_8)
            } else {
                // iTXt format: keyword\0compression\0method\0language\0translated\0text
                val nullIndex = data.indexOf(0.toByte())
                if (nullIndex < 0) return null

                val keyword = String(data, 0, nullIndex, Charsets.ISO_8859_1)
                if (keyword != "chara" && keyword != "ccv3") return null

                // Skip compression flag (1) + method (1) + language tag (variable\0)
                // + translated keyword (variable\0)
                var pos = nullIndex + 3 // skip null + compression + method
                // Skip language tag
                while (pos < data.size && data[pos] != 0.toByte()) pos++
                pos++ // skip null
                // Skip translated keyword
                while (pos < data.size && data[pos] != 0.toByte()) pos++
                pos++ // skip null

                if (pos >= data.size) return null
                val value = String(data, pos, data.size - pos, Charsets.UTF_8)
                // Try Base64 decode, fallback to raw
                try {
                    String(Base64.getDecoder().decode(value), Charsets.UTF_8)
                } catch (e: Exception) {
                    value
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract text chunk")
            null
        }
    }

    // ─── CharX Parsing ────────────────────────────────────────────────────────

    /**
     * Extract character data from a CharX (ZIP) archive.
     * CharX contains card.json at the root level.
     */
    private fun parseCharX(inputStream: InputStream): Character? {
        return try {
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "card.json" || entry.name.endsWith("/card.json")) {
                        val jsonStr = zip.readBytes().toString(Charsets.UTF_8)
                        return parseCharacterJson(jsonStr)
                    }
                    entry = zip.nextEntry
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse CharX")
            null
        }
    }

    // ─── JSON Parsing ─────────────────────────────────────────────────────────

    private fun parseJson(inputStream: InputStream): Character? {
        return try {
            val jsonStr = inputStream.readBytes().toString(Charsets.UTF_8)
            parseCharacterJson(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON character")
            null
        }
    }

    // ─── Character JSON → Domain Model ────────────────────────────────────────

    /**
     * Parse character JSON (V1/V2/V3 format) into a domain Character.
     *
     * V1: flat object with name, description, etc.
     * V2: { spec: "chara_card_v2", data: { name, ... } }
     * V3: { spec: "chara_card_v3", data: { name, ... } }
     */
    fun parseCharacterJson(jsonStr: String): Character? {
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject

            // Detect version and extract data object
            val spec = root["spec"]?.jsonPrimitive?.content ?: ""
            val specVersion = root["spec_version"]?.jsonPrimitive?.content ?: ""
            val dataObj: JsonObject = when {
                spec == "chara_card_v2" || spec == "chara_card_v3" -> {
                    root["data"]?.jsonObject ?: root
                }
                else -> root // V1: flat structure
            }

            val name = dataObj["name"]?.jsonPrimitive?.content ?: return null
            if (name.isBlank()) return null

            val alternateGreetings = try {
                dataObj["alternate_greetings"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }

            val tags = try {
                dataObj["tags"]?.jsonArray?.map {
                    it.jsonPrimitive.content
                } ?: emptyList()
            } catch (e: Exception) { emptyList() }

            val extensionsJson = try {
                dataObj["extensions"]?.toString() ?: "{}"
            } catch (e: Exception) { "{}" }

            val characterBookJson = try {
                dataObj["character_book"]?.toString() ?: ""
            } catch (e: Exception) { "" }

            val now = System.currentTimeMillis()

            Character(
                id = UUID.randomUUID().toString(),
                name = name,
                description = dataObj["description"]?.jsonPrimitive?.content ?: "",
                personality = dataObj["personality"]?.jsonPrimitive?.content ?: "",
                scenario = dataObj["scenario"]?.jsonPrimitive?.content ?: "",
                firstMessage = dataObj["first_mes"]?.jsonPrimitive?.content ?: "",
                alternateGreetings = alternateGreetings,
                exampleDialogue = dataObj["mes_example"]?.jsonPrimitive?.content ?: "",
                systemPrompt = dataObj["system_prompt"]?.jsonPrimitive?.content ?: "",
                postHistoryInstructions = dataObj["post_history_instructions"]
                    ?.jsonPrimitive?.content ?: "",
                creatorNotes = dataObj["creator_notes"]?.jsonPrimitive?.content ?: "",
                creator = dataObj["creator"]?.jsonPrimitive?.content ?: "",
                characterVersion = dataObj["character_version"]?.jsonPrimitive?.content ?: "",
                tags = tags,
                extensionsJson = extensionsJson,
                characterBookJson = characterBookJson,
                createdAt = now,
                updatedAt = now,
                rawJson = jsonStr,
                spec = spec,
                specVersion = specVersion
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse character JSON")
            null
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val pngSignature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        return bytes.take(8).toByteArray().contentEquals(pngSignature)
    }

    private fun isZip(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 0x50.toByte() &&
               bytes[1] == 0x4B.toByte() &&
               bytes[2] == 0x03.toByte() &&
               bytes[3] == 0x04.toByte()
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
               (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri, null, null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}