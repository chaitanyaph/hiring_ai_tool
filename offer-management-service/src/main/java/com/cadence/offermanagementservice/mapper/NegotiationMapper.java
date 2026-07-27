package com.cadence.offermanagementservice.mapper;

import com.cadence.offermanagementservice.dto.response.NegotiationResponse;
import com.cadence.offermanagementservice.entity.OfferNegotiation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NegotiationMapper {
    NegotiationResponse toResponse(OfferNegotiation negotiation);
}
