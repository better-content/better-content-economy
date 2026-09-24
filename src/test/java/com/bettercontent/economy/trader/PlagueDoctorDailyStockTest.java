package com.bettercontent.economy.trader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlagueDoctorDailyStockTest {
    @Test void exemptionRequiresCurrentDayAndExactSelectedAuthoredRow() {
        UUID doctor = UUID.fromString("15b31f1a-3d37-4c73-8a21-49a2e5b76f90");
        long day = 912L;
        var selected = PlagueDoctorCatalogue.selectRows(doctor, day, ignored -> true);
        var row = selected.get(0);
        var exact = PlagueDoctorCatalogue.signature(row);

        assertTrue(PlagueDoctorCatalogue.isAuthoredDailyOffer(0, selected.size(),
                PlagueDoctorCatalogue.VERSION, day, day, selected, exact));
        assertFalse(PlagueDoctorCatalogue.isAuthoredDailyOffer(0, selected.size(),
                PlagueDoctorCatalogue.VERSION, day, day - 1, selected, exact));
        assertFalse(PlagueDoctorCatalogue.isAuthoredDailyOffer(0, selected.size(),
                PlagueDoctorCatalogue.VERSION - 1, day, day, selected, exact));
        assertFalse(PlagueDoctorCatalogue.isAuthoredDailyOffer(selected.size(), selected.size(),
                PlagueDoctorCatalogue.VERSION, day, day, selected, exact));
        assertFalse(PlagueDoctorCatalogue.isAuthoredDailyOffer(0, 0,
                PlagueDoctorCatalogue.VERSION, day, day, selected, exact));
        assertFalse(PlagueDoctorCatalogue.isAuthoredDailyOffer(0, selected.size(),
                PlagueDoctorCatalogue.VERSION, day, day, selected,
                new PlagueDoctorCatalogue.DailyOfferSignature(exact.payment(), exact.paymentCount() + 1,
                        exact.secondPayment(), exact.secondPaymentCount(), exact.result(), exact.resultCount(),
                        exact.maxUses(), exact.xp())));
    }
}
