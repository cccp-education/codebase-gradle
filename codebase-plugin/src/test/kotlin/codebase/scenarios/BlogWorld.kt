package codebase.scenarios

import codebase.blog.BlogArticleData

class BlogWorld {
    var extractedArticles: List<BlogArticleData> = emptyList()
    var generatedFiles: List<java.io.File> = emptyList()
    var tmpDir: java.io.File? = null
    var blogDir: java.io.File? = null

    fun reset() {
        extractedArticles = emptyList()
        generatedFiles = emptyList()
        tmpDir = null
        blogDir = null
    }
}
