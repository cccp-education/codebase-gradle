package codebase.blog

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class EndSessionBlogTask : DefaultTask() {

    @get:InputDirectory
    abstract val foundryDir: DirectoryProperty

    @get:OutputDirectory
    abstract val blogDir: DirectoryProperty

    @get:Internal
    abstract val nextArticleNumber: Property<Int>

    @TaskAction
    fun execute() {
        val foundryRoot = foundryDir.get().asFile
        val blogRoot = blogDir.get().asFile
        val articleNum = nextArticleNumber.getOrElse(9999)

        val extractor = BlogArticleExtractor()
        val articles = extractor.extract(foundryRoot)

        if (articles.isEmpty()) {
            logger.warn("[endSessionBlog] No session files found in {}", foundryRoot.absolutePath)
            return
        }

        blogRoot.mkdirs()
        var number = articleNum

        for (article in articles) {
            val renderer = BlogArticleRenderer(articleNumber = number)
            renderer.render(article, blogRoot)
            logger.lifecycle("[endSessionBlog] Article $number généré : ${article.sessionNumber} — ${article.boroughName} : ${article.sessionTitle}")
            number++
        }

        logger.lifecycle("[endSessionBlog] ${articles.size} articles générés dans ${blogRoot.absolutePath}")
    }
}
