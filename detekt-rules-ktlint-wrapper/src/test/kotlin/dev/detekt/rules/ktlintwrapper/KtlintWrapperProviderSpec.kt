package dev.detekt.rules.ktlintwrapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtlintWrapperProviderSpec {

    @Test
    fun `provides the wrapped ktlint rules`() {
        assertThat(KtlintWrapperProvider().instance().rules).isNotEmpty()
    }
}
