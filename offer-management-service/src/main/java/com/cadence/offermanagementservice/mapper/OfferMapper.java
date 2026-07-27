package com.cadence.offermanagementservice.mapper;

import com.cadence.offermanagementservice.dto.response.OfferDetailResponse;
import com.cadence.offermanagementservice.dto.response.OfferListItemResponse;
import com.cadence.offermanagementservice.entity.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.List;

/** companyName/timeline/negotiations/documentGenerated are enriched by the service layer, not stored redundantly on the entity. benefits is stored as a comma-separated column and split/joined here. */
@Mapper(componentModel = "spring")
public interface OfferMapper {

    OfferListItemResponse toListItemResponse(Offer offer);

    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "benefits", expression = "java(splitBenefits(offer.getBenefits()))")
    @Mapping(target = "documentGenerated", ignore = true)
    @Mapping(target = "timeline", ignore = true)
    @Mapping(target = "negotiations", ignore = true)
    OfferDetailResponse toDetailResponse(Offer offer);

    default List<String> splitBenefits(String benefits) {
        if (benefits == null || benefits.isBlank()) {
            return List.of();
        }
        return Arrays.stream(benefits.split(",")).map(String::trim).toList();
    }

    default String joinBenefits(List<String> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return "";
        }
        return String.join(",", benefits);
    }
}
