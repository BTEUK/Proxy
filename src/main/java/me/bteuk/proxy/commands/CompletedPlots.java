package me.bteuk.proxy.commands;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CompletedPlots extends PlotListCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public CompletedPlots(String name, String description, OptionData... options) {
        super(name, description, "Completed Plots", "SELECT pd.id FROM plot_data AS pd INNER JOIN accept_data AS u ON pd.id=u.id WHERE pd.status='completed'%uuid%;", "There are currently no completed plots.", options);
    }
}
