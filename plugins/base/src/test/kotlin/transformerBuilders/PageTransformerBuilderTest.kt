package transformerBuilders;

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.pages.pageMapper
import org.jetbrains.dokka.transformers.pages.pageScanner
import org.jetbrains.dokka.transformers.pages.pageStructureTransformer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PageTransformerBuilderTest : AbstractCoreTest() {

    class ProxyPlugin(transformer: PageTransformer) : DokkaPlugin() {
        val pageTransformer by extending { CoreExtensions.pageTransformer with transformer }
    }

    @Test
    fun scannerTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }
        val list = mutableListOf<String>()

        var orig: PageNode? = null

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageScanner {
                list += name
            }))
        ) {
            pagesGenerationStage = {
                orig = it
            }
            pagesTransformationStage = { root ->
                list.assertCount(4, "Page list: ")
                orig?.let { root.assertTransform(it) }
            }
        }
    }

    @Test
    fun mapperTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }

        var orig: PageNode? = null

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageMapper {
                modified(name = name + "2")
            }))
        ) {
            pagesGenerationStage = {
                orig = it
            }
            pagesTransformationStage = {
                it.let { root ->
                    root.name.assertEqual("root2", "Root name: ")
                    orig?.let {
                        root.assertTransform(it) { node -> node.modified(name = node.name + "2") }
                    }
                }
            }
        }
    }

    @Test
    fun structureTransformerTest() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/transformerBuilder/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/transformerBuilder/Test.kt
            |package transformerBuilder
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration,
            pluginOverrides = listOf(ProxyPlugin(pageStructureTransformer {
                val ch = children.first()
                modified(
                    children = listOf(
                        ch,
                        RendererSpecificResourcePage("test", emptyList(), RenderingStrategy.DoNothing)
                    )
                )
            }))
        ) {
            pagesTransformationStage = { root ->
                root.children.assertCount(2, "Root children: ")
                root.children.first().name.assertEqual("transformerBuilder")
                root.children[1].name.assertEqual("test")
            }
        }
    }

    @Test
    fun `kotlin constructors tab should not exist`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }
        testInline(
            """
            |/src/main/kotlin/kotlinAsJavaPlugin/Test.kt
            |package kotlinAsJavaPlugin
            |
            |class Test(val xd: Int)
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = { root ->
                val content = root.children
                    .flatMap { it.children<ContentPage>() }
                    .map { it.content }.single().children
                    .filterIsInstance<ContentGroup>()
                    .single { it.dci.kind == ContentKind.Main }.children

                val constructorTabsCount = content.filter { it is ContentHeader }.flatMap {
                    it.children.filter { it is ContentText }
                }.count {
                    (it as? ContentText)?.text == "Constructors"
                }

                assertEquals(0, constructorTabsCount)
            }
        }
    }


    private fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    private fun <T> T.assertEqual(expected: T, prefix: String = "") = assert(this == expected) {
        "${prefix}Expected $expected, got $this"
    }

    private fun PageNode.assertTransform(expected: PageNode, block: (PageNode) -> PageNode = { it }): Unit = this.let {
        it.name.assertEqual(block(expected).name)
        it.children.zip(expected.children).forEach { (g, e) ->
            g.name.assertEqual(block(e).name)
            g.assertTransform(e, block)
        }
    }
}
