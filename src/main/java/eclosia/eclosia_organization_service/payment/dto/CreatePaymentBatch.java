package eclosia.eclosia_organization_service.payment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eclosia.eclosia_organization_service.common.jackson.CreatePaymentDtoListDeserializer;

import java.util.ArrayList;

@JsonDeserialize(using = CreatePaymentDtoListDeserializer.class)
public class CreatePaymentBatch extends ArrayList<CreatePaymentDto> {
}
