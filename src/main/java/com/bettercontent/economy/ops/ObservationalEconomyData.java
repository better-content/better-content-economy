package com.bettercontent.economy.ops;

import com.bettercontent.economy.spirit.CurrencyIdentity;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
    public static ObservationalEconomyData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(ObservationalEconomyData::load, ObservationalEconomyData::new, NAME); }
    public void recordRegion(String region, Map<CurrencyIdentity,Integer> values) { if (region == null || regional.size() >= 64 && !regional.containsKey(region)) return; var row = regional.computeIfAbsent(region, k -> new EnumMap<>(CurrencyIdentity.class)); values.forEach((k,v) -> { if(k != null && v != null && v > 0) row.merge(k,v,Integer::sum); }); setDirty(); }
    public void recordPurchase(CurrencyIdentity identity, int count) { if (identity != null && count > 0) { purchases.merge(identity,count,Integer::sum); setDirty(); } }
    public Map<String, Map<CurrencyIdentity,Integer>> regional() { var copy = new HashMap<String, Map<CurrencyIdentity,Integer>>(); regional.forEach((k,v) -> copy.put(k, Map.copyOf(v))); return Map.copyOf(copy); }
    public Map<CurrencyIdentity,Integer> purchases() { return Map.copyOf(purchases); }
    public static ObservationalEconomyData load(CompoundTag tag) { var d = new ObservationalEconomyData(); for(Tag t:tag.getList("Regions",Tag.TAG_COMPOUND)){var e=(CompoundTag)t; String n=e.getString("Name"); if(n.isEmpty())continue; var row=new EnumMap<CurrencyIdentity,Integer>(CurrencyIdentity.class); for(var i:CurrencyIdentity.values()){int v=e.getCompound("Values").getInt(i.id());if(v>0)row.put(i,v);} if(!row.isEmpty())d.regional.put(n,row);} var p=tag.getCompound("Purchases"); for(var i:CurrencyIdentity.values()){int v=p.getInt(i.id());if(v>0)d.purchases.put(i,v);} return d; }
    @Override public CompoundTag save(CompoundTag tag) { var regions=new ListTag(); regional.forEach((n,row)->{var e=new CompoundTag();e.putString("Name",n);var v=new CompoundTag();row.forEach((i,c)->v.putInt(i.id(),c));e.put("Values",v);regions.add(e);});tag.put("Regions",regions);var p=new CompoundTag();purchases.forEach((i,c)->p.putInt(i.id(),c));tag.put("Purchases",p);return tag; }
}
