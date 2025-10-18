package com.kuafuai.api.util;


import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@UtilityClass
public class ApiUtil {
    public static String interpolateString(String templateInput, Map<String, String> valuesMap) {
        return Optional.ofNullable(valuesMap).orElse(Collections.emptyMap()).entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .filter(entry -> entry.getValue() != null)
                .flatMap(entry -> {
                    final String key = entry.getKey();
                    final String value = entry.getValue();
                    return Stream.of(
                            new AbstractMap.SimpleEntry<>("${" + key + "}", value),
                            new AbstractMap.SimpleEntry<>("${{" + key + "}}", value),
                            new AbstractMap.SimpleEntry<>("{{" + key + "}}", value)
                    );
                })
                .map(explodedParam -> (Function<String, String>) s -> s.replace(explodedParam.getKey(), explodedParam.getValue()))
                .reduce(Function.identity(), Function::andThen)
                .apply(Objects.requireNonNull(templateInput, "templateInput is required"));
    }
}
