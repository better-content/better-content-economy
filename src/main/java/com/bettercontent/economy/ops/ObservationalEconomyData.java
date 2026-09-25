package com.bettercontent.economy.ops;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Aggregate owner observations; deliberately contains no player identity. */
public final class ObservationalEconomyData extends SavedData {
    private static final String NAME = "better_content_economy_observations";
    private final Map<String, EnumMap<CurrencyIdentity, Integer>> regional = new HashMap<>();
    private final EnumMap<CurrencyIdentity, Integer> purchases = new EnumMap<>(CurrencyIdentity.class);
    private final EnumMap<CurrencyIdentity, Integer> released = new EnumMap<>(CurrencyIdentity.class);
    private final Map<String, Integer> exchanges = new HashMap<>();
    private static final int MAX_EXCHANGES = 256;
    private long activity;
    public static ObservationalEconomyData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(ObservationalEconomyData::load, ObservationalEconomyData::new, NAME); }
    public void recordRegion(String region, Map<CurrencyIdentity,Integer> values) { if (region == null || regional.size() >= 64 && !regional.containsKey(region)) return; var row = regional.computeIfAbsent(region, k -> new EnumMap<>(CurrencyIdentity.class)); values.forEach((k,v) -> { if(k != null && v != null && v > 0) row.merge(k,v,Integer::sum); }); setDirty(); }
    public void recordPurchase(CurrencyIdentity identity, int count) { if (identity != null && count > 0) { purchases.merge(identity,count,Integer::sum); setDirty(); } }
    public void recordRelease(Map<CurrencyIdentity,Integer> values) { values.forEach((identity,count) -> { if (identity != null && count != null && count > 0) released.merge(identity,count,(a,b) -> a > Integer.MAX_VALUE - b ? Integer.MAX_VALUE : a + b); }); setDirty(); }
    public void recordExchange(CurrencyIdentity identity, int count, String merchant) {
        if (identity == null || count <= 0 || merchant == null || merchant.isBlank()) return;
        String key = identity.id() + "\u0000" + merchant;
        if (exchanges.size() >= MAX_EXCHANGES && !exchanges.containsKey(key)) return;
        exchanges.merge(key, count, (a, b) -> a > Integer.MAX_VALUE - b ? Integer.MAX_VALUE : a + b);
        setDirty();
    }
    public List<com.bettercontent.economy.ops.ObservationalSpiritExport.Exchange> exchanges() {
        var result = new ArrayList<com.bettercontent.economy.ops.ObservationalSpiritExport.Exchange>();
        exchanges.forEach((key, count) -> { int split = key.indexOf('\u0000'); if (split > 0 && count > 0) {
            CurrencyIdentity identity = CurrencyIdentity.fromItemId(new net.minecraft.resources.ResourceLocation("better_content_economy", key.substring(0, split) + "_spirit"));
            if (identity != null) result.add(new com.bettercontent.economy.ops.ObservationalSpiritExport.Exchange(identity, count, key.substring(split + 1)));
        }});
        return List.copyOf(result);
    }
    public void recordActivity() { if (activity < Long.MAX_VALUE) activity++; setDirty(); }
    public long activity() { return activity; }
    public Map<String, Map<CurrencyIdentity,Integer>> regional() { var copy = new HashMap<String, Map<CurrencyIdentity,Integer>>(); regional.forEach((k,v) -> copy.put(k, Map.copyOf(v))); return Map.copyOf(copy); }
    public Map<CurrencyIdentity,Integer> purchases() { return Map.copyOf(purchases); }
    public Map<CurrencyIdentity,Integer> released() { return Map.copyOf(released); }
    public static ObservationalEconomyData load(CompoundTag tag) { var d = new ObservationalEconomyData(); d.activity = Math.max(0, tag.getLong("Activity")); for(Tag t:tag.getList("Regions",Tag.TAG_COMPOUND)){var e=(CompoundTag)t; String n=e.getString("Name"); if(n.isEmpty())continue; var row=new EnumMap<CurrencyIdentity,Integer>(CurrencyIdentity.class); for(var i:CurrencyIdentity.values()){int v=e.getCompound("Values").getInt(i.id());if(v>0)row.put(i,v);} if(!row.isEmpty())d.regional.put(n,row);} var p=tag.getCompound("Purchases"); var released=tag.getCompound("Released"); for(var i:CurrencyIdentity.values()){int v=p.getInt(i.id());if(v>0)d.purchases.put(i,v);v=released.getInt(i.id());if(v>0)d.released.put(i,v);} for(Tag t:tag.getList("Exchanges",Tag.TAG_COMPOUND)){var e=(CompoundTag)t; String identity=e.getString("Identity"), merchant=e.getString("Merchant"); int count=e.getInt("Count"); if(!identity.isEmpty() && !merchant.isEmpty() && count>0 && d.exchanges.size()<MAX_EXCHANGES)d.exchanges.put(identity+"\u0000"+merchant,count);} return d; }
    @Override public CompoundTag save(CompoundTag tag) { tag.putLong("Activity", activity); var regions=new ListTag(); regional.forEach((n,row)->{var e=new CompoundTag();e.putString("Name",n);var v=new CompoundTag();row.forEach((i,c)->v.putInt(i.id(),c));e.put("Values",v);regions.add(e);});tag.put("Regions",regions);var p=new CompoundTag();purchases.forEach((i,c)->p.putInt(i.id(),c));tag.put("Purchases",p);var releasedTag=new CompoundTag();released.forEach((i,c)->releasedTag.putInt(i.id(),c));tag.put("Released",releasedTag);var rows=new ListTag();exchanges.forEach((key,count)->{int split=key.indexOf('\u0000');if(split>0){var e=new CompoundTag();e.putString("Identity",key.substring(0,split));e.putString("Merchant",key.substring(split+1));e.putInt("Count",count);rows.add(e);}});tag.put("Exchanges",rows);return tag; }
}
