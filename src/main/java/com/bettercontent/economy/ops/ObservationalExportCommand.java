package com.bettercontent.economy.ops;

import com.bettercontent.economy.config.EconomyConfig;
import com.bettercontent.economy.spirit.SpiritCreditData;
import com.bettercontent.economy.spirit.SpiritCreditLedger;
import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Permissioned server command for the owner observational export. */
public final class ObservationalExportCommand {
    private ObservationalExportCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("better_content_economy")
                .then(Commands.literal("export")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> export(context.getSource()))));
    }

    private static int export(CommandSourceStack source) {
        if (!EconomyConfig.observationalExportEnabled()) {
            source.sendFailure(Component.literal("Observational export is disabled by server config."));
            return 0;
        }
        var server = source.getServer();
        SpiritCreditData data = SpiritCreditData.get(server.overworld());
        var observations = ObservationalEconomyData.get(server.overworld());
        var snapshots = new ArrayList<ObservationalSpiritExport.CreditSnapshot>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpiritCreditLedger ledger = data.ledger(player.getUUID());
            snapshots.add(new ObservationalSpiritExport.CreditSnapshot(ledger.issued(), ledger.pending()));
        }
        String json = ObservationalSpiritExport.json(true, 2, snapshots, observations.exchanges(), observations.activity(), observations.regional(), observations.purchases());
        source.sendSuccess(() -> Component.literal(json), true);
        return snapshots.size();
    }
}
