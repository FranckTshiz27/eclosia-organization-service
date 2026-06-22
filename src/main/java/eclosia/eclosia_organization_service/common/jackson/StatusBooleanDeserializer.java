package eclosia.eclosia_organization_service.common.jackson;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class StatusBooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();

        if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
            return parser.getBooleanValue();
        }

        if (token == JsonToken.VALUE_STRING) {
            String rawValue = parser.getText();
            String normalizedValue = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);

            return switch (normalizedValue) {
                case "true", "1", "actif", "active" -> true;
                case "false", "0", "inactif", "inactive" -> false;
                default -> throw InvalidFormatException.from(
                        parser,
                        "Status must be one of: true, false, Actif, Inactif",
                        rawValue,
                        Boolean.class
                );
            };
        }

        if (token == JsonToken.VALUE_NUMBER_INT) {
            int numericValue = parser.getIntValue();
            if (numericValue == 1) {
                return true;
            }
            if (numericValue == 0) {
                return false;
            }
        }

        throw InvalidFormatException.from(
                parser,
                "Status must be one of: true, false, Actif, Inactif",
                parser.getText(),
                Boolean.class
        );
    }
}
