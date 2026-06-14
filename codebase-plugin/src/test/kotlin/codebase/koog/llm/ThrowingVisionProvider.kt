package codebase.koog.llm

class ThrowingVisionProvider : VisionProvider {
    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        throw IllegalStateException("Gemini API quota exceeded — simulated failure for fallback test")
    }
}
