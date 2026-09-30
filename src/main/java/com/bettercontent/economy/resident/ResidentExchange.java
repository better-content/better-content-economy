package com.bettercontent.economy.resident;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Finds and settles cycles of real goods with no currency or three-party limit. */
public final class ResidentExchange {
    private record Edge(LivingEntity recipient, LivingEntity supplier, ItemStack good) {}
    private ResidentExchange() {}

    public static boolean settle(ServerLevel level, LivingEntity origin) {
        List<LivingEntity> residents = level.getEntitiesOfClass(LivingEntity.class,
                origin.getBoundingBox().inflate(24), entity -> entity.isAlive()
                        && ResidentRules.isResident(entity)
                        && ResidentRules.worksite(entity).equals(ResidentRules.worksite(origin)));
        residents.sort(Comparator.comparing(LivingEntity::getUUID));
        if (residents.size() < 2) return false;
        if (residents.size() > 32) residents = residents.subList(0, 32);
        Map<LivingEntity, List<Edge>> edges = new HashMap<>();
        Map<LivingEntity, List<ResidentBarter.Offer>> available = new HashMap<>();
        for (LivingEntity seller : residents) available.put(seller, ResidentBarter.offers(level, seller));
        int visited = 0;
        for (LivingEntity buyer : residents) {
            ResidentState need = ResidentState.of(buyer);
            for (LivingEntity seller : residents) {
                if (buyer == seller) continue;
                if (++visited > 512) break;
                for (ResidentBarter.Offer offer : available.getOrDefault(seller, List.of())) {
                    ItemStack item = offer.recipe() == null ? offer.result().copyWithCount(1) : offer.result();
                    if (item.isEmpty() || !ResidentRules.needs(item, need)) continue;
                    edges.computeIfAbsent(buyer, ignored -> new ArrayList<>())
                            .add(new Edge(buyer, seller, item));
                    break;
                }
            }
        }
        for (LivingEntity start : residents) {
            List<Edge> path = new ArrayList<>();
            if (find(start, start, edges, new HashSet<>(), path) && commit(level, path)) return true;
        }
        return false;
    }

    private static boolean find(LivingEntity current, LivingEntity start, Map<LivingEntity, List<Edge>> edges,
                                Set<LivingEntity> seen, List<Edge> path) {
        if (!seen.add(current)) return false;
        for (Edge edge : edges.getOrDefault(current, List.of())) {
            if (edge.supplier() == start && !path.isEmpty()) { path.add(edge); return true; }
            if (seen.contains(edge.supplier())) continue;
            path.add(edge);
            if (find(edge.supplier(), start, edges, seen, path)) return true;
            path.remove(path.size() - 1);
        }
        seen.remove(current);
        return false;
    }

    private static boolean commit(ServerLevel level, List<Edge> cycle) {
        // Preview every recipe and transfer before committing any resident inventory.
        Map<LivingEntity, List<ItemStack>> staged = new HashMap<>();
        for (Edge edge : cycle) {
            List<ItemStack> after = ResidentBarter.preview(level, edge.supplier(), edge.good());
            if (after == null) return false;
            staged.put(edge.supplier(), after);
        }
        for (Edge edge : cycle) {
            List<ItemStack> recipient = staged.get(edge.recipient());
            if (recipient == null || !ResidentBarter.addTo(recipient, edge.good())) return false;
        }
        for (var row : staged.entrySet()) ResidentState.of(row.getKey()).replaceItems(row.getValue());
        for (Edge edge : cycle) {
            ResidentState state = ResidentState.of(edge.recipient());
            state.setDoing("Bartered for " + edge.good().getHoverName().getString());
            state.event("traded with " + edge.supplier().getDisplayName().getString()
                    + " for " + edge.good().getHoverName().getString());
        }
        return true;
    }
}
