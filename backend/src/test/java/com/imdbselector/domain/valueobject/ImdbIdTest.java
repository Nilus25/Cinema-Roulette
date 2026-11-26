package com.imdbselector.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImdbIdTest {

    @Test
    void acceptsValidImdbId() {
        var id = new ImdbId(" tt0111161 ");
        assertThat(id.value()).isEqualTo("tt0111161");
    }

    @Test
    void rejectsInvalidImdbId() {
        assertThatThrownBy(() -> new ImdbId("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
