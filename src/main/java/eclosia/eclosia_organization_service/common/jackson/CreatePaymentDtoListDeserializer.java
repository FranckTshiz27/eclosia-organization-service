package eclosia.eclosia_organization_service.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import eclosia.eclosia_organization_service.payment.dto.CreatePaymentBatch;
import eclosia.eclosia_organization_service.payment.dto.CreatePaymentDto;

import java.io.IOException;
import java.util.List;

public class CreatePaymentDtoListDeserializer extends JsonDeserializer<CreatePaymentBatch> {

    private static final TypeReference<List<CreatePaymentDto>> LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public CreatePaymentBatch deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonToken token = parser.currentToken();
        CreatePaymentBatch batch = new CreatePaymentBatch();

        if (token == JsonToken.START_ARRAY) {
            batch.addAll(mapper.readValue(parser, LIST_TYPE));
            return batch;
        }

        if (token == JsonToken.START_OBJECT) {
            batch.add(mapper.readValue(parser, CreatePaymentDto.class));
            return batch;
        }

        throw InvalidFormatException.from(
                parser,
                "Expected a payment object or an array of payments",
                parser.getText(),
                CreatePaymentBatch.class
        );
    }
}
