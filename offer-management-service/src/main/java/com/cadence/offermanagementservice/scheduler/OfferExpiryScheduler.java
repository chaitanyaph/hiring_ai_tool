package com.cadence.offermanagementservice.scheduler;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.entity.OfferActivityLog;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Sweeps SENT offers whose expiryDate has passed to EXPIRED -- the "Offer Expiry" functional requirement; no dedicated Figma tab for it, same as WITHDRAWN. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferExpiryScheduler {

    private final OfferRepository offerRepository;
    private final OfferActivityLogRepository offerActivityLogRepository;

    @Scheduled(fixedRateString = "${offer-management.expiry.sweep-fixed-rate-ms}")
    @Transactional
    public void sweepExpiredOffers() {
        List<Offer> due = offerRepository.findAllByStatusAndExpiryDateLessThan(OfferStatus.SENT, LocalDate.now());
        for (Offer offer : due) {
            offer.setStatus(OfferStatus.EXPIRED);
            offerRepository.save(offer);
            offerActivityLogRepository.save(OfferActivityLog.builder()
                    .offerId(offer.getId()).eventType(ActivityEventType.EXPIRED).details("Offer expired unanswered").build());
        }
        if (!due.isEmpty()) {
            log.info("Marked {} offer(s) as EXPIRED", due.size());
        }
    }
}
