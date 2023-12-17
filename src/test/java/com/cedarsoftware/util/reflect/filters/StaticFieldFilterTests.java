package com.cedarsoftware.util.reflect.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.cedarsoftware.util.reflect.filters.field.StaticFieldFilter;
import com.cedarsoftware.util.reflect.filters.models.CarEnumWithCustomFields;
import com.cedarsoftware.util.reflect.filters.models.ColorEnum;

class StaticFieldFilterTests {

    @Test
    void enumFilter_withEnumThatHasAdditionalFields() {
        StaticFieldFilter staticFieldFilter = new StaticFieldFilter();
        Field[] fields = CarEnumWithCustomFields.class.getDeclaredFields();

        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                assertThat(staticFieldFilter.filter(field))
                        .isTrue()
                        .withFailMessage("static items should be filtered.");
            } else {
                assertThat(staticFieldFilter.filter(field))
                        .isFalse()
                        .withFailMessage("non-static items should not be filtered.");
            }
        }
    }

    @Test
    void staticFilter_filtersAll_onEnumWithAllStatics() {
        StaticFieldFilter staticFieldFilter = new StaticFieldFilter();
        Field[] fields = ColorEnum.BLUE.getClass().getDeclaredFields();

        for (Field field : fields) {
            assertThat(staticFieldFilter.filter(field))
                    .isTrue()
                    .withFailMessage("non-static items should not be filtered.");
        }
    }
}