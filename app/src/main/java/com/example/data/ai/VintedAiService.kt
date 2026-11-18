package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.DutchVintedAd
import com.example.model.GarmentUserNotes
import com.example.model.PoseType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "VintedAiService"

class VintedAiService {

    private val imageModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(AspectRatio.PORTRAIT_2x3)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    private val vintedAdSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(
                description = "Attractive, searchable Dutch Vinted listing title without hashtags"
            ),
            "description" to Schema.string(
                description = "Extensive, honest Dutch Vinted sales description without hashtags"
            ),
            "brand" to Schema.string(
                description = "Exact brand ONLY if clearly legible on a label or provided by user; otherwise 'Niet zichtbaar op label'"
            ),
            "size" to Schema.string(
                description = "Exact size read from label or provided by user; otherwise 'Niet zichtbaar op label'"
            ),
            "colour" to Schema.string(
                description = "Accurate colour description in Dutch"
            ),
            "material" to Schema.string(
                description = "Material composition ONLY if verified from care label or user; otherwise 'Niet geverifieerd (zie foto's)'"
            ),
            "styleAndFit" to Schema.string(
                description = "Style and fit description in Dutch based on garment structure"
            ),
            "searchTerms" to Schema.string(
                description = "Natural comma-separated Dutch search terms without # hashtags"
            ),
            "suggestedAskingPrice" to Schema.string(
                description = "Suggested Vinted asking price in EUR (e.g. '€ 24,50')"
            ),
            "expectedSellingPrice" to Schema.string(
                description = "Realistic expected selling price range in EUR (e.g. '€ 18,00 - € 21,00')"
            ),
            "quickSalePrice" to Schema.string(
                description = "Quick-sale price in EUR (e.g. '€ 15,00')"
            ),
            "readyToCopyListing" to Schema.string(
                description = "Complete Dutch Vinted advertisement ready to copy and paste, zero hashtags"
            )
        )
    )

    private val textAdModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = vintedAdSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun generatePosePhoto(
        poseType: PoseType,
        modelRefBitmap: Bitmap,
        bedroomRefBitmap: Bitmap,
        garmentBitmaps: List<Bitmap>,
        customRoomNote: String,
        consistencyFirstPoseBitmap: Bitmap? = null
    ): Bitmap {
        val promptContent = content {
            text(
                """
                PROFESSIONAL VINTED AI FASHION PHOTOGRAPHY STUDIO.
                Generate ONE SINGLE independent photorealistic 2:3 portrait fashion photograph.
                STRICTLY FORBIDDEN: collages, triptychs, split-screens, contact sheets, borders, watermarks, or text overlays.

                1. MODEL IDENTITY REFERENCE (HIGHEST IDENTITY PRIORITY — IGNORE ITS BACKGROUND ROOM):
                Use ONLY the brunette woman's identity from the Model Identity Reference photo below. Maintain the exact same recognizable face, facial proportions, eyes, nose, warm cheerful smile, body proportions, height, and long brown hair naturally styled. Do NOT generate a different woman, and do NOT copy the room background from the Model Identity Reference photo.
                """.trimIndent()
            )
            image(modelRefBitmap)

            text(
                if (customRoomNote.isBlank()) {
                    """
                    2. BEDROOM SETTING REFERENCE (KEEP IDENTICAL ACROSS ALL THREE PHOTOGRAPHS):
                    Place the model inside the EXACT realistic Dutch-style bedroom shown in the Bedroom Setting Reference photo below. Keep the bed and bedding, tufted grey headboard and matching bench, window and sage blind, white shelving unit and dresser, framed wall art, pendant lamp, warm wood flooring, daylight lighting, colour palette, and room proportions consistent. Do not redesign or rearrange the bedroom.
                    """.trimIndent()
                } else {
                    """
                    2. CUSTOM ROOM SETTING REQUESTED BY USER:
                    $customRoomNote
                    Keep interior architecture, lighting, flooring, and proportions photorealistic and consistent across all poses.
                    """.trimIndent()
                }
            )
            if (customRoomNote.isBlank()) {
                image(bedroomRefBitmap)
            }

            if (consistencyFirstPoseBitmap != null) {
                text(
                    """
                    3. SESSION CONSISTENCY ANCHOR (IMAGE 1 FROM THIS SAME PHOTOSHOOT):
                    Match the exact model identity, exact garment appearance, and exact bedroom environment from this first shot:
                    """.trimIndent()
                )
                image(consistencyFirstPoseBitmap)
            }

            text(
                """
                4. AUTHORITATIVE GARMENT REFERENCE PHOTOS (ABSOLUTE PRODUCT PRIORITY):
                Reproduce this EXACT clothing item on the model with maximum fidelity:
                - Exact original colours, colour gradients, and patterns.
                - Exact knit structure, fabric texture, seams, collar, sleeves, hem, length, and fit.
                - NEVER redesign, simplify, embellish, or invent logos, labels, pockets, buttons, or details that are not present in the original garment photos.
                """.trimIndent()
            )
            garmentBitmaps.forEachIndexed { idx, bitmap ->
                text("Authoritative Garment Photo ${idx + 1}:")
                image(bitmap)
            }

            text(buildPoseInstruction(poseType))
        }

        val response = withTimeout(60_000L) {
            imageModel.generateContent(promptContent)
        }

        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()

        return imagePart?.image
            ?: throw IllegalStateException("The image model did not return an image for ${poseType.title}.")
    }

    private fun buildPoseInstruction(poseType: PoseType): String {
        val poseSection = when (poseType) {
            PoseType.STANDING_FRONT -> """
                5. SPECIFIC POSE REQUIREMENT — IMAGE 1 (STANDING FRONT VIEW):
                - Full-length standing photograph inside the bedroom.
                - Model standing naturally, facing the camera directly.
                - Entire garment clearly visible from collar/shoulders to hem and sleeves.
                - Natural, attractive fashion pose with a friendly, subtly attractive smile.
                - Clothing is the clear main focus.
            """.trimIndent()

            PoseType.STANDING_BACK -> """
                5. SPECIFIC POSE REQUIREMENT — IMAGE 2 (STANDING BACK VIEW):
                - Full-length standing photograph inside the same bedroom.
                - Model turned completely around (180 degrees) with her back facing the camera.
                - Show the ACTUAL BACK of the garment clearly (back neckline/collar, back seams/pattern, full length, and sleeves).
                - Move her long brown hair neatly forward over one shoulder or tucked away from the back of the garment so the entire back design is completely unobstructed.
            """.trimIndent()

            PoseType.SEATED_LIFESTYLE -> """
                5. SPECIFIC POSE REQUIREMENT — IMAGE 3 (SEATED LIFESTYLE VIEW):
                - Model sitting naturally on the edge of the bed in the same bedroom.
                - Relaxed, feminine, confident lifestyle pose with a warm friendly smile.
                - Show naturally how the garment drapes and falls when seated and worn.
                - Keep the garment clearly visible, unobstructed by arms or hair.
            """.trimIndent()
        }

        return """
            $poseSection

            6. PHOTOGRAPHIC QUALITY REQUIREMENTS:
            - Portrait orientation (2:3 aspect ratio).
            - Authentic indoor fashion photography, natural daylight, realistic skin texture (never plastic), correct anatomy and hands, crisp fabric folds and sharp knit/textile details.
            - Must look like a genuine, high-converting Vinted fashion photoshoot, NOT obvious AI art.
            - Generate ONLY this single photograph. Never create a collage or split frame.
        """.trimIndent()
    }

    suspend fun generateDutchVintedAd(
        garmentBitmaps: List<Bitmap>,
        userNotes: GarmentUserNotes
    ): DutchVintedAd {
        val promptContent = content {
            text(
                """
                Act as a Dutch Vinted sales specialist and fashion copywriter.
                Inspect the attached original garment photographs carefully and write a complete, accurate, ready-to-paste Dutch Vinted advertisement.

                STRICT TRUTHFULNESS RULES:
                - NEVER invent material composition, brand details, condition, or measurements that are not clearly visible in the photos or explicitly provided in the user notes below.
                - If brand is not legible on a label and not provided below, set brand to "Niet zichtbaar op label".
                - If size is not legible on a label and not provided below, set size to "Niet zichtbaar op label".
                - If material composition is not legible on a care label, set material to "Niet geverifieerd (zie foto's)".
                - DO NOT USE HASHTAGS (#) ANYWHERE in the title, description, searchTerms, or readyToCopyListing.
                - Write natural, warm, persuasive Dutch tailored to Vinted NL/BE buyers.

                USER-PROVIDED VERIFIED NOTES (may be blank if relying solely on photos):
                - Verified Brand: ${userNotes.brand.ifBlank { "(not provided — inspect label photo only)" }}
                - Verified Label Size: ${userNotes.sizeFromLabel.ifBlank { "(not provided — inspect label photo only)" }}
                - Verified Condition: ${userNotes.condition.ifBlank { "(not provided — describe only visible state conservatively)" }}

                Include in readyToCopyListing:
                - Attractive searchable Dutch title
                - Extensive Dutch sales description with natural search terms woven in
                - Verified specs (Merk, Maat, Kleur, Materiaal if verified, Stijl & Pasvorm, Staat)
                - Zero hashtags anywhere.
                """.trimIndent()
            )
            garmentBitmaps.forEachIndexed { idx, bitmap ->
                text("Original Garment Photo ${idx + 1}:")
                image(bitmap)
            }
        }

        val response = withTimeout(60_000L) {
            textAdModel.generateContent(promptContent)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Gemini returned an empty advertisement response.")

        return parseDutchAdJson(jsonText)
    }

    private fun parseDutchAdJson(jsonString: String): DutchVintedAd {
        val obj = JSONObject(jsonString)
        return DutchVintedAd(
            title = obj.optString("title", "").replace("#", "").trim(),
            description = obj.optString("description", "").replace("#", "").trim(),
            brand = obj.optString("brand", "Niet zichtbaar op label").trim(),
            size = obj.optString("size", "Niet zichtbaar op label").trim(),
            colour = obj.optString("colour", "").trim(),
            material = obj.optString("material", "Niet geverifieerd (zie foto's)").trim(),
            styleAndFit = obj.optString("styleAndFit", "").trim(),
            searchTerms = obj.optString("searchTerms", "").replace("#", "").trim(),
            suggestedAskingPrice = obj.optString("suggestedAskingPrice", "").trim(),
            expectedSellingPrice = obj.optString("expectedSellingPrice", "").trim(),
            quickSalePrice = obj.optString("quickSalePrice", "").trim(),
            readyToCopyListing = obj.optString("readyToCopyListing", "").replace("#", "").trim()
        )
    }
}
