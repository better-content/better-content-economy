package com.bettercontent.economy.trader;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
final class LocalMarketValidationTest {
    private final UUID id = UUID.randomUUID();
    @Test void acceptsCurrentNearbyStock() { assertTrue(LocalMarketValidation.accepts(new LocalMarketValidation.Request(id,"a",4,3), id,new LocalMarketValidation.Offer("a",4,1),8)); }
    @Test void rejectsStaleRemoteMissingOrExhausted() {
        var request=new LocalMarketValidation.Request(id,"a",4,3);
        assertFalse(LocalMarketValidation.accepts(request,id,new LocalMarketValidation.Offer("a",5,1),8));
        assertFalse(LocalMarketValidation.accepts(request,id,new LocalMarketValidation.Offer("a",4,0),8));
        assertFalse(LocalMarketValidation.accepts(request,UUID.randomUUID(),new LocalMarketValidation.Offer("a",4,1),8));
        assertFalse(LocalMarketValidation.accepts(new LocalMarketValidation.Request(id,"a",4,9),id,new LocalMarketValidation.Offer("a",4,1),8));
    }
}
