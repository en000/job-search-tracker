package local.jobtracker.controllers;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
class JsonDates {
    @Bean
    SimpleModule trackerDateFormats() {
        return new SimpleModule("tracker-date-formats")
                .addDeserializer(LocalDate.class, strict(LocalDate.class, "\\d{4}-\\d{2}-\\d{2}", LocalDate::parse, "YYYY-MM-DD"))
                .addDeserializer(Instant.class, strict(Instant.class,
                        "\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,9})?Z",
                        Instant::parse, "an ISO UTC timestamp ending in Z"));
    }

    private static <T> JsonDeserializer<T> strict(Class<T> type, String pattern, Function<String, T> convert, String description) {
        Pattern syntax = Pattern.compile(pattern);
        return new JsonDeserializer<>() {
            @Override
            public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                if (parser.hasToken(JsonToken.VALUE_STRING)) {
                    String text = parser.getText();
                    if (syntax.matcher(text).matches()) {
                        try {
                            return convert.apply(text);
                        } catch (DateTimeParseException ignored) {
                            // Report out-of-range values as field errors.
                        }
                    }
                }
                return context.reportInputMismatch(type, "Expected %s.", description);
            }
        };
    }
}
