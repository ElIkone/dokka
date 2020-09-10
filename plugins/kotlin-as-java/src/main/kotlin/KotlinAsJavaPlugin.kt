package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.translators.KotlinAsJavaDocumentableFilteringStrategies
import org.jetbrains.dokka.kotlinAsJava.translators.KotlinAsJavaDocumentableToPageTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDocumentableTransformer by extending {
        CoreExtensions.documentableTransformer with KotlinAsJavaDocumentableTransformer()
    }

    val javaSignatureProvider by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.signatureProvider providing { ctx ->
            JavaSignatureProvider(ctx.single(dokkaBasePlugin.commentsToContentConverter), ctx.logger)
        } override dokkaBasePlugin.kotlinSignatureProvider
    }

    val defaultDocumentableFilteringStrategies by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        dokkaBasePlugin.documentableFilteringStrategies with KotlinAsJavaDocumentableFilteringStrategies override
                dokkaBasePlugin.defaultDocumentableFilteringStrategies
    }
    
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        val dokkaBasePlugin = plugin<DokkaBase>()
        CoreExtensions.documentableToPageTranslator providing { ctx ->
            KotlinAsJavaDocumentableToPageTranslator(
                ctx.single(dokkaBasePlugin.commentsToContentConverter),
                ctx.single(dokkaBasePlugin.signatureProvider),
                ctx
            )
        } override dokkaBasePlugin.documentableToPageTranslator
    }
}
